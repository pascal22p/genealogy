package models.forms

import java.time.Instant

import models.queryData.EventDetailQueryData
import models.EventType.EventType
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.data.Forms.optional
import play.api.data.Forms.text

final case class EventDetailForm(
    base: Int,
    place: Option[Int],
    addr_id: Option[Int],
    events_tag: String,
    events_details_descriptor: String,
    events_details_gedcom_date: String,
    events_details_age: String,
    events_details_cause: String
) {
  def toEventDetailQueryData(eventType: EventType, ownerId: Int): EventDetailQueryData = {
    EventDetailQueryData(
      base,
      0,
      place,
      addr_id,
      events_details_descriptor,
      events_details_gedcom_date,
      events_details_age,
      events_details_cause,
      None,
      None,
      None,
      None,
      None,
      Instant.now,
      Some(events_tag),
      None,
      eventType,
      0,
      Some(ownerId),
      None
    )
  }
}

object EventDetailForm {

  def unapply(
      u: EventDetailForm
  ): Some[(Int, Option[Int], Option[Int], String, String, String, String, String)] = Some(
    (
      u.base,
      u.place,
      u.addr_id,
      u.events_tag,
      u.events_details_descriptor,
      u.events_details_gedcom_date,
      u.events_details_age,
      u.events_details_cause
    )
  )

  val eventDetailForm: Form[EventDetailForm] = Form(
    mapping(
      "base"                       -> number(min = 1),
      "place"                      -> optional(number),
      "addr_id"                    -> optional(number),
      "events_tag"                 -> text,
      "events_details_descriptor"  -> text,
      "events_details_gedcom_date" -> text,
      "events_details_age"         -> text,
      "events_details_cause"       -> text
    )(EventDetailForm.apply)(EventDetailForm.unapply)
  )
}
