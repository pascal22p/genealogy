package models.forms

import java.time.Instant

import models.SourCitationType
import play.api.data.FieldMapping
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.nonEmptyText
import play.api.data.Forms.number
import play.api.data.Forms.of
import play.api.data.Forms.optional
import play.api.data.Forms.text

final case class SourRecordForm(
    auth: String,
    title: String,
    abbr: String,
    publ: String,
    agnc: String,
    rin: String,
    repoCaln: String,
    repoMedi: String,
    parentId: Int,
    parentType: SourCitationType.SourCitationType
)

object SourRecordForm {

  def unapply(
      u: SourRecordForm
  ): Option[(String, String, String, String, String, String, String, String, Int, SourCitationType.SourCitationType)] =
    Some((u.auth, u.title, u.abbr, u.publ, u.agnc, u.rin, u.repoCaln, u.repoMedi, u.parentId, u.parentType))

  val sourRecordForm: Form[SourRecordForm] = Form(
    mapping(
      "auth"       -> text,
      "title"      -> text,
      "abbr"       -> text,
      "publ"       -> text,
      "agnc"       -> text,
      "rin"        -> text,
      "repoCaln"   -> text,
      "repoMedi"   -> text,
      "parentId"   -> number,
      "parentType" -> of[SourCitationType.SourCitationType](SourCitationType.formatter)
    )(SourRecordForm.apply)(SourRecordForm.unapply)
  )
}
