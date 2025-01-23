package services.gedcom

import javax.inject.Inject
import javax.inject.Singleton

import anorm.Row
import anorm.SQL
import anorm.SimpleSql
import cats.data.Ior
import models.gedcom.GedcomIndiBlock
import models.gedcom.GedcomNode
import play.api.db.Database

@Singleton
class GedcomImportService @Inject() (
    gedcomCommonParser: GedcomCommonParser,
    gedcomIndividualParser: GedcomIndividualParser,
    gedcomHashIdTable: GedcomHashIdTable,
    db: Database
) {

  def gedcom2sql(gedcomString: String, dbId: Int) = {
    val nodes = gedcomCommonParser.getTree(gedcomString)
    val sqls  = convertTree2SQL(nodes, dbId)

    db.withTransaction { implicit conn =>
      sqls.foreach { sql =>
        sql.execute()
      }
    }
    true
  }

  def convertTree2SQL(nodes: List[GedcomNode], base: Int): List[SimpleSql[Row]] = {
    val indis: Ior[List[String], List[GedcomIndiBlock]] = nodes
      .filter(_.name == "INDI")
      .map(gedcomIndividualParser.readIndiBlock)
      .foldLeft(Ior.Right(List.empty): Ior[List[String], List[GedcomIndiBlock]]) {
        case (listIndividuals, individual) =>
          listIndividuals.combine(individual.map(i => List(i)))
      }

    val warnings = indis.left.getOrElse(List.empty)
    warnings.foreach { warning =>
      println(warning)
    }

    val startTransaction: List[SimpleSql[Row]] = List(
      SQL("SET FOREIGN_KEY_CHECKS=1").on(),
      SQL("START TRANSACTION").on(),
      SQL("select * from genea_individuals WHERE indi_id > 0 LOCK IN SHARE MODE").on(),
      SQL(
        "SELECT `AUTO_INCREMENT` INTO @startIndi FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'genealogie' AND TABLE_NAME = 'genea_individuals'"
      ).on(),
      SQL(
        "SELECT `AUTO_INCREMENT` INTO @startEvent FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'genealogie' AND TABLE_NAME = 'genea_events_details'"
      ).on()
    )

    val commitTransaction: List[SimpleSql[Row]] = List(SQL("COMMIT;").on())

    val sqlHead =
      s"INSERT INTO genea_individuals (indi_id, base, indi_nom, indi_prenom, indi_sexe, indi_npfx, indi_givn, indi_nick, indi_spfx, indi_nsfx, indi_resn) VALUES\n"
    val indiSqls: List[SimpleSql[Row]] = indis.right
      .getOrElse(List.empty)
      .map { indi =>
        val nameRegex = "([^/]*)/([^/]*)/(.*)".r
        val (firstname, surname) = indi.nameStructure.name match {
          case nameRegex(firstname, surname, other) => (firstname, surname + " " + other)
          case _                                    => ("", "")
        }
        SQL(
          s"""$sqlHead ({indi_id} + @startIndi, {base}, {surname}, {firstname}, {indi_sex}, {indi_npfx}, {indi_givn}, {indi_nick}, {indi_spfx}, {indi_nsfx}, {indi_resn});"""
        )
          .on(
            "indi_id"   -> indi.id,
            "base"      -> base,
            "surname"   -> surname,
            "firstname" -> firstname,
            "indi_sex"  -> indi.sex,
            "indi_npfx" -> indi.nameStructure.npfx,
            "indi_givn" -> indi.nameStructure.givn,
            "indi_nick" -> indi.nameStructure.nick,
            "indi_spfx" -> indi.nameStructure.spfx,
            "indi_nsfx" -> indi.nameStructure.nsfx,
            "indi_resn" -> indi.resn.map(resn => s"$resn")
          )
      }

    val eventsSql = indis.right.getOrElse(List.empty).flatMap { indi =>
      indi.events.flatMap { event =>
        val eventId = gedcomHashIdTable.getEventId
        List(
          SQL(
            "INSERT INTO `genea_events_details` (`events_details_id`, `place_id`, `addr_id`, `events_details_descriptor`, `events_details_gedcom_date`, `events_details_age`, `events_details_cause`, `jd_count`, `jd_precision`, `jd_calendar`, `events_details_famc`, `events_details_adop`, `base`) " +
              "VALUES ({events_details_id} + @startEvent, {place_id}, {addr_id}, {events_details_descriptor}, {events_details_gedcom_date}, {events_details_age}, {events_details_cause}, {jd_count}, {jd_precision}, {jd_calendar}, {events_details_famc}, {events_details_adop}, {base})"
          )
            .on(
              "events_details_id"          -> eventId,
              "place_id"                   -> Option.empty[String],
              "addr_id"                    -> Option.empty[String],
              "events_details_descriptor"  -> "",
              "events_details_gedcom_date" -> event.date,
              "events_details_age"         -> "",
              "events_details_cause"       -> "",
              "jd_count"                   -> Option.empty[String],
              "jd_precision"               -> Option.empty[String],
              "jd_calendar"                -> Option.empty[String],
              "events_details_famc"        -> Option.empty[String],
              "events_details_adop"        -> Option.empty[String],
              "base"                       -> base,
            ),
          SQL(
            "INSERT INTO `rel_indi_events` (`events_details_id`, `indi_id`, `events_tag`, `events_attestation`)" +
              "VALUES ({events_details_id}+ @startEvent, {indi_id} + @startIndi, {events_tag}, {events_attestation})"
          )
            .on(
              "events_details_id"  -> eventId,
              "indi_id"            -> indi.id,
              "events_tag"         -> event.tag,
              "events_attestation" -> Option.empty[String]
            )
        )
      }

    }

    startTransaction ++ indiSqls ++ eventsSql ++ commitTransaction
  }

}
