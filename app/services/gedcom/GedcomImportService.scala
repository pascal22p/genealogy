package services.gedcom

import javax.inject.Inject
import javax.inject.Singleton

import scala.annotation.tailrec
import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.DurationInt
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import anorm.BatchSql
import anorm.Row
import anorm.SQL
import anorm.SimpleSql
import config.AppConfig
import models.gedcom.GedcomFamilyBlock
import models.gedcom.GedcomNode
import models.AuthenticatedRequest
import models.DatabaseExecutionContext
import play.api.db.Database

@Singleton
class GedcomImportService @Inject() (
    gedcomCommonParser: GedcomCommonParser,
    gedcomIndividualParser: GedcomIndividualParser,
    gedcomFamilyParser: GedcomFamilyParser,
    gedcomEventParser: GedcomEventParser,
    gedcomPlaceParser: GedcomPlaceParser,
    gedcomHashIdTable: GedcomHashIdTable,
    config: AppConfig,
    db: Database,
    databaseExecutionContext: DatabaseExecutionContext
)(implicit ec: ExecutionContext) {

  def findAllPlaces(nodes: Seq[GedcomNode]): Seq[GedcomNode] = {
    @tailrec
    def loop(todo: Seq[GedcomNode], acc: Seq[GedcomNode]): Seq[GedcomNode] =
      todo match {
        case Nil                  => acc.reverse
        case Seq(head, tail @ _*) =>
          val acc2 = if (head.name == "PLAC") head +: acc else acc
          loop(head.children ++ tail, acc2)
      }

    loop(nodes, Nil)
  }

  def insertGedcomInDatabase(gedcomPath: String, dbId: Int, jobId: String)(
      implicit request: AuthenticatedRequest[?]
  ): Future[Boolean] = {
    val gedcomObject = gedcomCommonParser.getTree(gedcomPath)
    gedcomHashIdTable.updateJobStatus(jobId, "File parsed")
    val sqlStatements = convertTree2SQL(gedcomObject.nodes, dbId, jobId)

    Future {
      gedcomHashIdTable.updateJobStatus(jobId, "Writing database in progress")
      db.withTransaction { implicit conn =>
        startTransaction.map(_.execute()).toSeq.reduce(_ && _)

        sqlStatements.zipWithIndex.foldLeft(0) {
          case (result, (sql, idx)) =>
            if (idx <= 10000 && idx % 1000 == 0) {
              gedcomHashIdTable.updateJobStatus(jobId, s"$idx sql statements executed"): Unit
            }
            if (idx > 10000 && idx % 10000 == 0) {
              gedcomHashIdTable.updateJobStatus(jobId, s"$idx sql statements executed"): Unit
            }
            sql.execute().sum + result
        }

        commitTransaction.map(_.execute()).toSeq.reduce(_ && _)
      }
    }(using databaseExecutionContext)
  }

  val startTransaction: Iterator[SimpleSql[Row]] = Iterator(
    SQL("SET FOREIGN_KEY_CHECKS=1").on(),
    SQL("START TRANSACTION").on(),
    SQL("select * from genea_individuals WHERE indi_id > 0 LOCK IN SHARE MODE").on(),
    SQL(
      s"SELECT `AUTO_INCREMENT` INTO @startIndi FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '${config.databaseName}' AND TABLE_NAME = 'genea_individuals'"
    ).on(),
    SQL(
      s"SELECT `AUTO_INCREMENT` INTO @startEvent FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '${config.databaseName}' AND TABLE_NAME = 'genea_events_details'"
    ).on(),
    SQL(
      s"SELECT `AUTO_INCREMENT` INTO @startFamily FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '${config.databaseName}' AND TABLE_NAME = 'genea_familles'"
    ).on(),
    SQL(
      s"SELECT `AUTO_INCREMENT` INTO @startPlace FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '${config.databaseName}' AND TABLE_NAME = 'genea_place'"
    ).on()
  )

  val commitTransaction: Iterator[SimpleSql[Row]] = Iterator(SQL("COMMIT;").on())

  def convertTree2SQL(nodes: Seq[GedcomNode], base: Int, jobId: String)(
      implicit request: AuthenticatedRequest[?]
  ): Iterator[BatchSql] = {
    val indis = nodes
      .filter(_.name == "INDI")
      .flatMap(node => gedcomIndividualParser.readIndiBlock(node, jobId).right)

    val families: TrieMap[Int, GedcomFamilyBlock] = TrieMap.from(
      nodes
        .filter(_.name == "FAM")
        .flatMap(node => gedcomFamilyParser.readFamilyBlock(node, jobId).right.map(fam => fam.id -> fam))
    )

    Await.result(
      Future.traverse(indis) { indi =>
        Future {
          indi.famsLinks.foreach { famLink =>
            families.updateWith(famLink) {
              case Some(family) =>
                (family.husb, family.wife, indi.sex) match {
                  case (Some(husbId), _, _) if husbId == indi.id                                 => Some(family)
                  case (_, Some(wifeId), _) if wifeId == indi.id                                 => Some(family)
                  case (Some(husbId), Some(wifeId), _) if wifeId != indi.id && husbId != indi.id =>
                    throw new RuntimeException(
                      s"Family ${family.id} already has both husband and wife defined, cannot add indi ${indi.id} as either"
                    )
                  case (None, _, "M")        => Some(family.copy(husb = Some(indi.id)))
                  case (Some(_), None, "M")  => Some(family.copy(wife = Some(indi.id)))
                  case (_, None, "F")        => Some(family.copy(wife = Some(indi.id)))
                  case (None, Some(_), "F")  => Some(family.copy(husb = Some(indi.id)))
                  case (None, _, _)          => Some(family.copy(husb = Some(indi.id)))
                  case (_, None, _)          => Some(family.copy(wife = Some(indi.id)))
                  case (Some(_), Some(_), _) =>
                    throw new RuntimeException(
                      s"Family ${family.id} already has both husband and wife defined, cannot add indi ${indi.id} as either"
                    )
                }

              case None =>
                if (indi.sex == "F") {
                  Some(GedcomFamilyBlock(famLink, Some(indi.id), None, Set.empty, List.empty, None))
                } else {
                  Some(GedcomFamilyBlock(famLink, None, Some(indi.id), Set.empty, List.empty, None))
                }
            }
          }

          indi.famcLinks.foreach { famLink =>
            families.updateWith(famLink) {
              case Some(family) => Some(family.copy(children = family.children + indi.id))
              case None         => Some(GedcomFamilyBlock(famLink, None, None, Set(indi.id), List.empty, None))
            }
          }
        }
      },
      2.minutes
    )

    val indiSqls =
      indis.iterator.grouped(100).flatMap { indis =>
        Iterator.single(gedcomIndividualParser.gedcomIndiBlock2Sql(indis, base)) ++
          gedcomEventParser.gedcomIndividualEventBlock2Sql(indis, base, jobId)
      }
    val familySqls =
      families.values.iterator.grouped(100).flatMap { families =>
        Iterator.single(gedcomFamilyParser.gedcomFamilyBlock2Sql(families, base)) ++
          Iterator.single(gedcomFamilyParser.gedcomChildren2Sql(families)) ++
          gedcomEventParser.gedcomFamilyEventBlock2Sql(families, base, jobId)
      }
    val places                         = findAllPlaces(nodes).flatMap(_.content)
    val placesBlocks                   = gedcomPlaceParser.readPlaceBlocks(places, jobId)
    val placesSqls: Iterator[BatchSql] = gedcomPlaceParser.placeBlocks2Sql(placesBlocks, base).iterator

    placesSqls ++ indiSqls ++ familySqls
  }

  def convertTree2SQLWarnings(nodes: Seq[GedcomNode], jobId: String): Iterator[String] = {
    val indisWarnings: Iterator[String] = nodes.iterator
      .filter(_.name == "INDI")
      .flatMap(node => gedcomIndividualParser.readIndiBlock(node, jobId).left.getOrElse(List.empty))

    val familiesWarnings: Iterator[String] = nodes.iterator
      .filter(_.name == "FAM")
      .flatMap(node => gedcomFamilyParser.readFamilyBlock(node, jobId).left.getOrElse(List.empty))

    val ignoredContent: Iterator[String] = nodes.iterator
      .filterNot(node => List("INDI", "FAM").contains(node.name))
      .map { node =>
        s"Line ${node.lineNumber}: `${node.line}` in root is not supported"
      }

    indisWarnings ++ familiesWarnings ++ ignoredContent
  }

}
