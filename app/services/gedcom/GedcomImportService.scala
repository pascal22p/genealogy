package services.gedcom

import javax.inject.Inject
import javax.inject.Singleton

import scala.annotation.tailrec
import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.DurationInt
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

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
    config: AppConfig,
    db: Database,
    databaseExecutionContext: DatabaseExecutionContext
)(implicit ec: ExecutionContext) {

  def findAllPlaces(nodes: List[GedcomNode]): List[GedcomNode] = {
    @tailrec
    def loop(todo: List[GedcomNode], acc: List[GedcomNode]): List[GedcomNode] =
      todo match {
        case Nil          => acc.reverse
        case head :: tail =>
          val acc2 = if (head.name == "PLAC") head :: acc else acc
          loop(head.children ++ tail, acc2)
      }

    loop(nodes, Nil)
  }

  def insertGedcomInDatabase(gedcomPath: String, dbId: Int)(
      implicit request: AuthenticatedRequest[?]
  ): Future[Boolean] = {
    val gedcomObject  = gedcomCommonParser.getTree(gedcomPath)
    val sqlStatements = convertTree2SQL(gedcomObject.nodes, dbId)

    Future {
      db.withTransaction { implicit conn =>
        sqlStatements.map(_.execute()).reduce(_ && _)
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

  def convertTree2SQL(nodes: List[GedcomNode], base: Int)(
      implicit request: AuthenticatedRequest[?]
  ): Iterator[SimpleSql[Row]] = {
    val indis = nodes
      .filter(_.name == "INDI")
      .flatMap(node => gedcomIndividualParser.readIndiBlock(node).right)

    val families: TrieMap[Int, GedcomFamilyBlock] = TrieMap.from(
      nodes
        .filter(_.name == "FAM")
        .flatMap(node => gedcomFamilyParser.readFamilyBlock(node).right.map(fam => fam.id -> fam))
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

    val indiSqls            = indis.iterator.map(indi => gedcomIndividualParser.gedcomIndiBlock2Sql(indi, base))
    val individualEventsSql =
      indis.iterator.flatMap(indi => gedcomEventParser.gedcomIndividualEventBlock2Sql(indi.events, base, indi.id))
    val familySqls      = families.iterator.map(family => gedcomFamilyParser.gedcomFamilyBlock2Sql(family._2, base))
    val childrenSqls    = families.iterator.flatMap(family => gedcomFamilyParser.gedcomChildren2Sql(family._2))
    val familyEventsSql = families.iterator.flatMap(family =>
      gedcomEventParser.gedcomFamilyEventBlock2Sql(family._2.events, base, family._2.id)
    )
    val places       = findAllPlaces(nodes).flatMap(_.content)
    val placesBlocks = gedcomPlaceParser.readPlaceBlocks(places)
    val placesSqls   = gedcomPlaceParser.placeBlocks2Sql(placesBlocks, base).iterator

    startTransaction ++ placesSqls ++ indiSqls ++ individualEventsSql ++ familySqls ++ childrenSqls ++ familyEventsSql ++ commitTransaction
  }

  def convertTree2SQLWarnings(nodes: List[GedcomNode]): Iterator[String] = {
    val indisWarnings: Iterator[String] = nodes.iterator
      .filter(_.name == "INDI")
      .flatMap(node => gedcomIndividualParser.readIndiBlock(node).left.getOrElse(List.empty))

    val familiesWarnings: Iterator[String] = nodes.iterator
      .filter(_.name == "FAM")
      .flatMap(node => gedcomFamilyParser.readFamilyBlock(node).left.getOrElse(List.empty))

    val ignoredContent: Iterator[String] = nodes.iterator
      .filterNot(node => List("INDI", "FAM").contains(node.name))
      .map { node =>
        s"Line ${node.lineNumber}: `${node.line}` in root is not supported"
      }

    indisWarnings ++ familiesWarnings ++ ignoredContent
  }

}
