package services.gedcom

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.Future

import anorm.Row
import anorm.SQL
import anorm.SimpleSql
import cats.data.Ior
import config.AppConfig
import models.gedcom.GedcomFamilyBlock
import models.gedcom.GedcomIndiBlock
import models.gedcom.GedcomNode
import models.DatabaseExecutionContext
import play.api.db.Database

@Singleton
class GedcomImportService @Inject() (
    gedcomCommonParser: GedcomCommonParser,
    gedcomIndividualParser: GedcomIndividualParser,
    gedcomFamilyParser: GedcomFamilyParser,
    gedcomEventParser: GedcomEventParser,
    config: AppConfig,
    db: Database,
    databaseExecutionContext: DatabaseExecutionContext
) {

  def gedcom2sql(gedcomString: String, dbId: Int): Future[Boolean] = Future {
    val nodes = gedcomCommonParser.getTree(gedcomString)
    val sqls  = convertTree2SQL(nodes, dbId)

    db.withTransaction { implicit conn =>
      sqls
        .map { sql =>
          sql.execute()
        }
        .reduce(_ && _)
    }
  }(databaseExecutionContext)

  val startTransaction: List[SimpleSql[Row]] = List(
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
    ).on()
  )

  val commitTransaction: List[SimpleSql[Row]] = List(SQL("COMMIT;").on())

  def convertTree2SQL(nodes: List[GedcomNode], base: Int): List[SimpleSql[Row]] = {
    val indis: Ior[List[String], List[GedcomIndiBlock]] = nodes
      .filter(_.name == "INDI")
      .map(gedcomIndividualParser.readIndiBlock)
      .foldLeft(Ior.Right(List.empty): Ior[List[String], List[GedcomIndiBlock]]) {
        case (listIndividuals, individual) =>
          listIndividuals.combine(individual.map(i => List(i)))
      }

    val families: Ior[List[String], List[GedcomFamilyBlock]] = nodes
      .filter(_.name == "FAM")
      .map(gedcomFamilyParser.readFamilyBlock)
      .foldLeft(Ior.Right(List.empty): Ior[List[String], List[GedcomFamilyBlock]]) {
        case (listFamily, family) =>
          listFamily.combine(family.map(i => List(i)))
      }

    val indiSqls: List[SimpleSql[Row]] = indis.right
      .getOrElse(List.empty)
      .map { indi =>
        gedcomIndividualParser.gedcomIndiBlock2Sql(indi, base)
      }

    val individualEventsSql: List[SimpleSql[Row]] =
      indis.right
        .getOrElse(List.empty)
        .flatMap(indi => gedcomEventParser.gedcomIndividualEventBlock2Sql(indi.events, base, indi.id))

    val familySqls: List[SimpleSql[Row]] = families.right
      .getOrElse(List.empty)
      .map { family =>
        gedcomFamilyParser.gedcomFamilyBlock2Sql(family, base)
      }

    val familyEventsSql: List[SimpleSql[Row]] =
      families.right
        .getOrElse(List.empty)
        .flatMap(family => gedcomEventParser.gedcomFamilyEventBlock2Sql(family.events, base, family.id))

    val ignoredContent: Ior[List[String], Map[String, String]] = Ior.Left(
      nodes
        .filterNot(node => List("INDI", "FAM").contains(node.name))
        .foldLeft(List.empty[String]) {
          case (result, node) =>
            result ++ List(s"Line ${node.lineNumber}: `${node.line}` is not supported")
        }
    )

    val warnings =
      indis.left.getOrElse(List.empty) ++ families.left.getOrElse(List.empty) ++ ignoredContent.left.getOrElse(
        List.empty
      )
    warnings.foreach { warning =>
      println(warning)
    }

    startTransaction ++ indiSqls ++ individualEventsSql ++ familySqls ++ familyEventsSql ++ commitTransaction
  }

}
