package models

import java.time.Instant

import config.AppConfig
import models.forms.EventDetailForm
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
    privacyRestriction: Option[ResnType.ResnType],
    sourCitations: List[SourCitation] = List.empty
) {
  def formatDate(
      implicit messages: Messages,
      authenticatedRequest: AuthenticatedRequest[?],
      appConfig: AppConfig
  ): String = {
    val dateRegex               = ".*([0-9]{4}).*".r
    val isAllowedToSee: Boolean = authenticatedRequest.localSession.sessionData.userData.fold(false)(_.seePrivacy)

    events_details_gedcom_date match {
      case dateRegex(year) if year.toInt > 1900 && !isAllowedToSee => appConfig.redactedMask
      case _ =>
        CalendarConstants.allKeywords
          .foldLeft(events_details_gedcom_date) {
            case (formattedDate, replace) =>
              replace._1.replaceAllIn(formattedDate, messages(replace._2))
          }
          .trim
    }
  }

  def toForm: EventDetailForm =
    EventDetailForm(
      base,
      place.map(_.id),
      addr_id,
      tag.getOrElse(""),
      events_details_descriptor,
      events_details_gedcom_date,
      events_details_age,
      events_details_cause
    )

  def fromForm(form: EventDetailForm, allPlaces: List[Place]): EventDetail = EventDetail(
    base,
    events_details_id,
    form.place.map(formId => allPlaces.find(_.id == formId)).flatten,
    form.addr_id,
    form.events_details_descriptor,
    form.events_details_gedcom_date,
    form.events_details_age,
    form.events_details_cause,
    jd_count,
    jd_precision,
    jd_calendar,
    events_details_famc,
    events_details_adop,
    Instant.now,
    Some(form.events_tag),
    description,
    eventType,
    sourCount,
    ownerId,
    privacyRestriction,
    sourCitations
  )
}

object EventDetail {
  def apply(
      eventDetailQueryData: EventDetailQueryData,
      place: Option[Place],
      sourCitations: List[SourCitation]
  ): EventDetail = {
    new EventDetail(
      eventDetailQueryData.dbId,
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
