package models.forms

import java.time.Instant

import anorm._
import anorm.SqlParser._
import models.EventDetail
import models.EventType
import models.EventType.EventType
import models.MaleSex
import models.PersonDetails
import models.Place
import models.Sex
import models.SourCitation
import play.api.data.format.Formats._
import play.api.data.FieldMapping
import play.api.data.Form
import play.api.data.Forms.ignored
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
)

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
