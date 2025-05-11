package models.forms

import java.time.Instant

import models.SourCitationQueryData
import models.SourCitationType.SourCitationType
import models.SourRecord
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.data.Forms.optional
import play.api.data.Forms.text

final case class SourCitationForm(
    date: String,
    even: String,
    role: String,
    submitter: String,
    text: String,
    page: String,
    quay: Option[Int],
    recordId: Option[Int]
) {
  def toSourCitationQueryData(ownerId: Int, dbId: Int, ownerType: SourCitationType): SourCitationQueryData =
    SourCitationQueryData(
      id = 0,
      record = recordId.map(id => SourRecord(id, "", "", "", "", "", "", None, "", "", Instant.now())),
      page = page,
      even = even,
      role = role,
      dates = date,
      text = text,
      quay = quay,
      subm = submitter,
      timestamp = Instant.now(),
      ownerId = Some(ownerId),
      sourceType = ownerType,
      dbId = dbId
    )
}

object SourCitationForm {

  def unapply(
      u: SourCitationForm
  ): Option[(String, String, String, String, String, String, Option[Int], Option[Int])] = Some(
    (u.date, u.even, u.role, u.submitter, u.text, u.page, u.quay, u.recordId)
  )

  val sourCitationForm: Form[SourCitationForm] = Form(
    mapping(
      "date"      -> text,
      "even"      -> text,
      "role"      -> text,
      "submitter" -> text,
      "text"      -> text,
      "page"      -> text,
      "quay"      -> optional(number),
      "recordId"  -> optional(number)
    )(SourCitationForm.apply)(SourCitationForm.unapply)
  )
}
