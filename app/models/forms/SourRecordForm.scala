package models.forms

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.data.Forms.optional
import play.api.data.Forms.text
import utils.isRelativeUrl

final case class SourRecordForm(
    auth: String,
    title: String,
    abbr: String,
    publ: String,
    agnc: String,
    rin: String,
    repoCaln: String,
    repoMedi: String,
    repoId: Option[Int],
    returnUrl: String
)

object SourRecordForm {

  def unapply(
      u: SourRecordForm
  ): Option[
    (
        String,
        String,
        String,
        String,
        String,
        String,
        String,
        String,
        Option[Int],
        String
    )
  ] =
    Some((u.auth, u.title, u.abbr, u.publ, u.agnc, u.rin, u.repoCaln, u.repoMedi, u.repoId, u.returnUrl))

  val sourRecordForm: Form[SourRecordForm] = Form(
    mapping(
      "auth"      -> text,
      "title"     -> text,
      "abbr"      -> text,
      "publ"      -> text,
      "agnc"      -> text,
      "rin"       -> text,
      "repoCaln"  -> text,
      "repoMedi"  -> text,
      "repoId"    -> optional(number),
      "returnUrl" -> text
        .transform(
          _.trim,
          identity
        )
        .verifying(
          "returnUrl must be a relative URL",
          _.isRelativeUrl
        )
    )(SourRecordForm.apply)(SourRecordForm.unapply)
  )
}
