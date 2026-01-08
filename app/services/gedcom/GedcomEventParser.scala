package services.gedcom

import anorm.BatchSql
import anorm.NamedParameter
import cats.data.Ior
import com.google.inject.Inject
import com.google.inject.Singleton
import models.gedcom.GedcomEventBlock
import models.gedcom.GedcomFamilyBlock
import models.gedcom.GedcomIndiBlock
import models.gedcom.GedcomNode

@Singleton
class GedcomEventParser @Inject() (gedcomHashIdTable: GedcomHashIdTable) {

  def readEventBlock(node: GedcomNode): Ior[Seq[String], GedcomEventBlock] = {
    val eventTag   = node.name
    val eventDate  = node.children.find(_.name == "DATE").flatMap(_.content)
    val eventPlace = node.children.find(_.name == "PLAC").flatMap(_.content.filter(_.trim.nonEmpty).map(_.trim))

    val ignoredContent: Seq[String] = node.children
      .filterNot(child => Seq("DATE", "PLAC").contains(child.name))
      .map { node =>
        s"Line ${node.lineNumber}: `${node.line}` " +
          s"in event is not supported"
      }
    val ignoredPlaceContent: Seq[String] = node.children.filter(_.name == "PLAC").flatMap { place =>
      place.children.map { node =>
        s"Line ${node.lineNumber}: `${node.line}` in place from event is not supported"
      }
    }

    Ior.Both(
      ignoredContent ++ ignoredPlaceContent,
      GedcomEventBlock(eventTag, eventDate.getOrElse(""), eventPlace)
    )
  }

  def gedcomIndividualEventBlock2Sql(
      listIndis: Seq[GedcomIndiBlock],
      base: Int,
      jobId: String
  ): Seq[BatchSql] = {
    val eventDetailsStatement =
      "INSERT INTO `genea_events_details` (`events_details_id`, `place_id`, `addr_id`, `events_details_descriptor`, `events_details_gedcom_date`, `events_details_age`, `events_details_cause`, `jd_count`, `jd_precision`, `jd_calendar`, `events_details_famc`, `events_details_adop`, `base`) " +
        "VALUES ({events_details_id} + @startEvent, {place_id} + @startPlace, {addr_id}, {events_details_descriptor}, {events_details_gedcom_date}, {events_details_age}, {events_details_cause}, {jd_count}, {jd_precision}, {jd_calendar}, {events_details_famc}, {events_details_adop}, {base})"
    val linkEventStatement =
      "INSERT INTO `rel_indi_events` (`events_details_id`, `indi_id`, `events_tag`, `events_attestation`)" +
        "VALUES ({events_details_id}+ @startEvent, {indi_id} + @startIndi, {events_tag}, {events_attestation})"
    val (eventDetailsParameters, linkEventsParameters) = listIndis.flatMap { indi =>
      indi.events.map { event =>
        val eventId = gedcomHashIdTable.getEventId(jobId)
        val placeId = event.place.map(s => gedcomHashIdTable.getPlaceIdFromString(jobId, s.trim))
        (
          Seq[NamedParameter](
            "events_details_id"          -> eventId,
            "place_id"                   -> placeId,
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
          Seq[NamedParameter](
            "events_details_id"  -> eventId,
            "indi_id"            -> indi.id,
            "events_tag"         -> event.tag,
            "events_attestation" -> Option.empty[String]
          )
        )
      }
    }.unzip

    if (eventDetailsParameters.isEmpty) {
      Seq.empty
    } else {
      Seq(
        BatchSql(eventDetailsStatement, eventDetailsParameters.head, eventDetailsParameters.tail*),
        BatchSql(linkEventStatement, linkEventsParameters.head, linkEventsParameters.tail*)
      )
    }
  }

  def gedcomFamilyEventBlock2Sql(
      listFamilies: Seq[GedcomFamilyBlock],
      base: Int,
      jobId: String
  ): Iterator[BatchSql] = {
    val eventDetailsStatement =
      "INSERT INTO `genea_events_details` (`events_details_id`, `place_id`, `addr_id`, `events_details_descriptor`, `events_details_gedcom_date`, `events_details_age`, `events_details_cause`, `jd_count`, `jd_precision`, `jd_calendar`, `events_details_famc`, `events_details_adop`, `base`) " +
        "VALUES ({events_details_id} + @startEvent, {place_id}, {addr_id}, {events_details_descriptor}, {events_details_gedcom_date}, {events_details_age}, {events_details_cause}, {jd_count}, {jd_precision}, {jd_calendar}, {events_details_famc}, {events_details_adop}, {base})"
    val linkEventsStatement =
      "INSERT INTO `rel_familles_events` (`events_details_id`, `familles_id`, `events_tag`, `events_attestation`)" +
        "VALUES ({events_details_id}+ @startEvent, {familles_id} + @startFamily, {events_tag}, {events_attestation})"

    val (eventDetailsParameters, linkEventsParameters) = listFamilies.flatMap { family =>
      family.events.map { event =>
        val eventId = gedcomHashIdTable.getEventId(jobId)
        (
          Seq[NamedParameter](
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
          Seq[NamedParameter](
            "events_details_id"  -> eventId,
            "familles_id"        -> family.id,
            "events_tag"         -> event.tag,
            "events_attestation" -> Option.empty[String]
          )
        )
      }
    }.unzip

    if (eventDetailsParameters.isEmpty) {
      Iterator.empty
    } else {
      Iterator(
        BatchSql(eventDetailsStatement, eventDetailsParameters.head, eventDetailsParameters.tail*),
        BatchSql(linkEventsStatement, linkEventsParameters.head, linkEventsParameters.tail*)
      )
    }
  }

}
