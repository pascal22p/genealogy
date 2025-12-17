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
    val nodes   = gedcomCommonParser.getTree(gedcomString)
    val sqlsIor = convertTree2SQL(nodes, dbId)

    db.withTransaction { implicit conn =>
      sqlsIor
        .getOrElse(List.empty)
        .map { sql =>
          sql.execute()
        }
        .reduce(_ && _)
    }
  }(using databaseExecutionContext)

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

  def convertTree2SQL(nodes: List[GedcomNode], base: Int): Ior[List[String], List[SimpleSql[Row]]] = {
    val indisIor: Ior[List[String], List[GedcomIndiBlock]] = nodes
      .filter(_.name == "INDI")
      .map(gedcomIndividualParser.readIndiBlock)
      .foldLeft(Ior.Right(List.empty): Ior[List[String], List[GedcomIndiBlock]]) {
        case (listIndividuals, individual) =>
          listIndividuals.combine(individual.map(i => List(i)))
      }

    val familiesIor: Ior[List[String], List[GedcomFamilyBlock]] = nodes
      .filter(_.name == "FAM")
      .map(gedcomFamilyParser.readFamilyBlock)
      .foldLeft(Ior.Right(List.empty): Ior[List[String], List[GedcomFamilyBlock]]) {
        case (listFamily, family) =>
          listFamily.combine(family.map(i => List(i)))
      }

    val familiesWithFamsFamcFromIndiIor = (for {
      indis    <- indisIor
      families <- familiesIor.map(_.map(family => family.id -> family).toMap)
    } yield {
      indis.foldLeft(families) { (familiesAcc, indi) =>
        val withFams = indi.famsLinks.foldLeft(familiesAcc) { (familiesAccInner, famLink) =>
          familiesAccInner.get(famLink) match {
            case Some(family) =>
              val newFamily = (family.husb, family.wife, indi.sex) match {
                case (Some(husbId), _, _) if husbId == indi.id                                 => family
                case (_, Some(wifeId), _) if wifeId == indi.id                                 => family
                case (Some(husbId), Some(wifeId), _) if wifeId != indi.id && husbId != indi.id =>
                  throw new RuntimeException(
                    s"Family ${family.id} already has both husband and wife defined, cannot add indi ${indi.id} as either"
                  )
                case (None, _, "M")        => family.copy(husb = Some(indi.id))
                case (Some(_), None, "M")  => family.copy(wife = Some(indi.id))
                case (_, None, "F")        => family.copy(wife = Some(indi.id))
                case (None, Some(_), "F")  => family.copy(husb = Some(indi.id))
                case (None, _, _)          => family.copy(husb = Some(indi.id))
                case (_, None, _)          => family.copy(wife = Some(indi.id))
                case (Some(_), Some(_), _) =>
                  throw new RuntimeException(
                    s"Family ${family.id} already has both husband and wife defined, cannot add indi ${indi.id} as either"
                  )
              }
              familiesAccInner.updated(famLink, newFamily)

            case None =>
              if (indi.sex == "F") {
                familiesAccInner.updated(
                  famLink,
                  GedcomFamilyBlock(famLink, Some(indi.id), None, List.empty, List.empty, None)
                )
              } else {
                familiesAccInner.updated(
                  famLink,
                  GedcomFamilyBlock(famLink, None, Some(indi.id), List.empty, List.empty, None)
                )
              }
          }
        }

        indi.famsLinks.foldLeft(withFams) { (familiesAccInner, famLink) =>
          familiesAccInner.get(famLink) match {
            case Some(family) =>
              val newFamily = (family.husb, family.wife, indi.sex) match {
                case (Some(husbId), _, _) if husbId == indi.id                                 => family
                case (_, Some(wifeId), _) if wifeId == indi.id                                 => family
                case (Some(husbId), Some(wifeId), _) if wifeId != indi.id && husbId != indi.id =>
                  throw new RuntimeException(
                    s"Family ${family.id} already has both husband and wife defined, cannot add indi ${indi.id} as either"
                  )
                case (None, _, "M")        => family.copy(husb = Some(indi.id))
                case (Some(_), None, "M")  => family.copy(wife = Some(indi.id))
                case (_, None, "F")        => family.copy(wife = Some(indi.id))
                case (None, Some(_), "F")  => family.copy(husb = Some(indi.id))
                case (None, _, _)          => family.copy(husb = Some(indi.id))
                case (_, None, _)          => family.copy(wife = Some(indi.id))
                case (Some(_), Some(_), _) =>
                  throw new RuntimeException(
                    s"Family ${family.id} already has both husband and wife defined, cannot add indi ${indi.id} as either"
                  )
              }
              familiesAccInner.updated(famLink, newFamily)

            case None =>
              if (indi.sex == "F") {
                familiesAccInner.updated(
                  famLink,
                  GedcomFamilyBlock(famLink, Some(indi.id), None, List.empty, List.empty, None)
                )
              } else {
                familiesAccInner.updated(
                  famLink,
                  GedcomFamilyBlock(famLink, None, Some(indi.id), List.empty, List.empty, None)
                )
              }
          }
        }
      }
    }).map(_.values.toList)

    val indiSqls: List[SimpleSql[Row]] = indisIor.right
      .getOrElse(List.empty)
      .map { indi =>
        gedcomIndividualParser.gedcomIndiBlock2Sql(indi, base)
      }

    val individualEventsSql: List[SimpleSql[Row]] =
      indisIor.right
        .getOrElse(List.empty)
        .flatMap(indi => gedcomEventParser.gedcomIndividualEventBlock2Sql(indi.events, base, indi.id))

    val familySqls: List[SimpleSql[Row]] = familiesWithFamsFamcFromIndiIor.right
      .getOrElse(List.empty)
      .map { family =>
        gedcomFamilyParser.gedcomFamilyBlock2Sql(family, base)
      }

    val familyEventsSql: List[SimpleSql[Row]] =
      familiesWithFamsFamcFromIndiIor.right
        .getOrElse(List.empty)
        .flatMap(family => gedcomEventParser.gedcomFamilyEventBlock2Sql(family.events, base, family.id))

    val ignoredContent: Ior[List[String], Map[String, String]] = Ior.Left(
      nodes
        .filterNot(node => List("INDI", "FAM").contains(node.name))
        .foldLeft(List.empty[String]) {
          case (result, node) =>
            result ++ List(s"Line ${node.lineNumber}: `${node.line}` in root is not supported")
        }
    )

    val warnings =
      indisIor.left.getOrElse(List.empty) ++ familiesWithFamsFamcFromIndiIor.left.getOrElse(
        List.empty
      ) ++ ignoredContent.left
        .getOrElse(
          List.empty
        )
    warnings.foreach { warning =>
      println(warning)
    }

    Ior.Both(
      warnings,
      startTransaction ++ indiSqls ++ individualEventsSql ++ familySqls ++ familyEventsSql ++ commitTransaction
    )
  }

}
