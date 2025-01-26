package services.gedcom

import anorm.Row
import anorm.SQL
import anorm.SimpleSql
import cats.data.Ior
import com.google.inject.Inject
import com.google.inject.Singleton
import models.gedcom.GedcomEventBlock
import models.gedcom.GedcomNode

@Singleton
class GedcomEventParser @Inject() (gedcomHashIdTable: GedcomHashIdTable) {

  def readEventBlock(node: GedcomNode): Ior[List[String], GedcomEventBlock] = {
    val eventTag  = node.name
    val eventDate = node.children.find(_.name == "DATE").flatMap(_.content)

    val ignoredContent: List[String] = node.children
      .filterNot(child => child.name == "DATE")
      .map { node =>
        s"Line ${node.lineNumber}: `${node.line}` is not supported"
      }
    Ior.Both(ignoredContent, GedcomEventBlock(eventTag, eventDate.getOrElse("")))
  }

  def gedcomIndividualEventBlock2Sql(
      listEvent: List[GedcomEventBlock],
      base: Int,
      individualId: Int
  ): List[SimpleSql[Row]] = {
    listEvent.flatMap { event =>
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
            "indi_id"            -> individualId,
            "events_tag"         -> event.tag,
            "events_attestation" -> Option.empty[String]
          )
      )
    }
  }

  def gedcomFamilyEventBlock2Sql(
      listEvent: List[GedcomEventBlock],
      base: Int,
      familyId: Int
  ): List[SimpleSql[Row]] = {
    listEvent.flatMap { event =>
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
          "INSERT INTO `rel_familles_events` (`events_details_id`, `familles_id`, `events_tag`, `events_attestation`)" +
            "VALUES ({events_details_id}+ @startEvent, {familles_id} + @startFamily, {events_tag}, {events_attestation})"
        )
          .on(
            "events_details_id"  -> eventId,
            "familles_id"        -> familyId,
            "events_tag"         -> event.tag,
            "events_attestation" -> Option.empty[String]
          )
      )
    }
  }

}
