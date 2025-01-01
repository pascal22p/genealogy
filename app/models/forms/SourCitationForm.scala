package models.forms

import java.time.Instant

import play.api.data.FieldMapping
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.data.Forms.optional
import play.api.data.Forms.text
import models.SourCitationType.IndividualSourCitation
 
final case class SourCitationForm(
    date: String,
    even: String,
    role: String,
    submitter: String,
    text: String,
    page: String,
    quay: Option[Int]
)

object SourCitationForm {

  def unapply(u: SourCitationForm): Option[(String, String, String, String, String, String, Option[Int])] = Some((u.date, u.even, u.role, u.submitter, u.text, u.page, u.quay))

  val sourCitationForm: Form[SourCitationForm] = Form(
    mapping(
      "date"         -> text,
      "even"       -> text,
      "role"      -> text,
      "submitter"       -> text,
      "text"       -> text,
      "page"      -> text,
      "quay"      -> optional(number)
    )(SourCitationForm.apply)(SourCitationForm.unapply)
  )
}
