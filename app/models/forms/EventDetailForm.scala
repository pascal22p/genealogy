package models.forms

import java.time.Instant

import anorm._
import anorm.SqlParser._
import models.MaleSex
import models.PersonDetails
import models.Sex
import play.api.data.format.Formats._
import play.api.data.FieldMapping
import play.api.data.Form
import play.api.data.Forms.ignored
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.data.Forms.optional
import play.api.data.Forms.text
import models.EventDetail
import models.Place
import models.EventType.EventType
import models.SourCitation
import models.EventType

final case class EventDetailForm(
  base: Int,
  events_details_id: Int,
  place: Option[Int],
  addr_id: Option[Int],
  events_tag: String,
  events_details_descriptor: String,
  events_details_gedcom_date: String,
  events_details_age: String,
  events_details_cause: String,
  eventType: EventType
)

object EventDetailForm {

  def apply(
    base: Int, 
    events_details_id: Int, 
    place: Option[Int], 
    addr_id: Option[Int], 
    events_tag: String, 
    events_details_descriptor: String, 
    events_details_gedcom_date: String, 
    events_details_age: String, 
    events_details_cause: String, 
    eventType: String): EventDetailForm = 
    new EventDetailForm(
      base, 
      events_details_id, 
      place, 
      addr_id, 
      events_tag, 
      events_details_descriptor, 
      events_details_gedcom_date, 
      events_details_age, 
      events_details_cause, 
      EventType.fromString(eventType))

  def unapply(
      u: EventDetailForm
  ): Some[(Int, Int, Option[Int], Option[Int], String, String, String, String, String, String)] = Some(
    (
      u.base,
      u.events_details_id,
      u.place,
      u.addr_id,
      u.events_tag,
      u.events_details_descriptor,
      u.events_details_gedcom_date,
      u.events_details_age,
      u.events_details_cause,
      s"${u.eventType}"
    )
  )

  val eventDetailForm: Form[EventDetailForm] = Form(
    mapping(
      "base"               -> number,
      "events_details_id"  -> number,
      "place"          -> optional(number),
      "addr_id"            -> optional(number),
      "events_tag" -> text,
      "events_details_descriptor"                -> text,
      "events_details_gedcom_date"    -> text,
      "events_details_age"      -> text,
      "events_details_cause"         -> text,
      "eventType" -> text
    )(EventDetailForm.apply)(EventDetailForm.unapply)
  )
}
