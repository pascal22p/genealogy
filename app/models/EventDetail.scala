package models

import java.time.Instant

import models.queryData.EventDetailQueryData
import models.EventType.EventType
import play.api.i18n.Messages
import utils.CalendarConstants

final case class EventDetail(
    base: Int,
    events_details_id: Int,
    place: Option[Place],
    addr_id: Option[Int],
    events_details_descriptor: String,
    events_details_gedcom_date: String,
    events_details_age: String,
    events_details_cause: String,
    jd_count: Option[Int],
    jd_precision: Option[Int],
    jd_calendar: Option[String],
    events_details_famc: Option[Int],
    events_details_adop: Option[String],
    events_details_timestamp: Instant,
    tag: Option[String],
    description: Option[String],
    eventType: EventType,
    sourCount: Int,
    ownerId: Option[Int],
    privacyRestriction: Option[String],
    sourCitations: List[SourCitationQueryData] = List.empty
) {
  def formatDate(implicit messages: Messages): String = {
    CalendarConstants.allKeywords
      .foldLeft(events_details_gedcom_date) {
        case (formattedDate, replace) =>
          replace._1.replaceAllIn(formattedDate, messages(replace._2))
      }
      .trim
  }
}

object EventDetail {
  def apply(
      eventDetailQueryData: EventDetailQueryData,
      place: Option[Place],
      sourCitations: List[SourCitationQueryData]
  ): EventDetail = {
    new EventDetail(
      eventDetailQueryData.base,
      eventDetailQueryData.events_details_id,
      place,
      eventDetailQueryData.addr_id,
      eventDetailQueryData.events_details_descriptor,
      eventDetailQueryData.events_details_gedcom_date,
      eventDetailQueryData.events_details_age,
      eventDetailQueryData.events_details_cause,
      eventDetailQueryData.jd_count,
      eventDetailQueryData.jd_precision,
      eventDetailQueryData.jd_calendar,
      eventDetailQueryData.events_details_famc,
      eventDetailQueryData.events_details_adop,
      eventDetailQueryData.events_details_timestamp,
      eventDetailQueryData.tag,
      eventDetailQueryData.description,
      eventDetailQueryData.eventType,
      eventDetailQueryData.sourCount,
      eventDetailQueryData.ownerId,
      eventDetailQueryData.resn,
      sourCitations = sourCitations
    )
  }
}
