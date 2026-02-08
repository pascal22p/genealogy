package models.forms

import models.journeyCache.UserAnswersItem
import play.api.data.Form
import play.api.data.Forms.boolean
import play.api.data.Forms.mapping
import play.api.data.Forms.optional
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.libs.json.OFormat
import play.api.libs.json.OWrites

final case class TrueOrFalseForm(
    trueOrFalse: Boolean
) extends UserAnswersItem

object TrueOrFalseForm {

  def unapply(
      u: TrueOrFalseForm
  ): Some[Boolean] = Some(
    u.trueOrFalse
  )

  val trueOrFalseForm: Form[TrueOrFalseForm] = Form(
    mapping(
      "trueOrFalse" -> optional(boolean)
        .verifying("error.required", _.isDefined)
        .transform[Boolean](_.getOrElse(false), Some(_))
    )(TrueOrFalseForm.apply)(TrueOrFalseForm.unapply)
  )

  def cyaWrites(using messages: Messages): OWrites[TrueOrFalseForm] =
    OWrites { form =>
      Json.obj(
        "trueOrFalse" ->
          messages(if (form.trueOrFalse) "Yes" else "No")
      )
    }

  def cyaFormat(using messages: Messages): OFormat[TrueOrFalseForm] = OFormat(
    Json.reads[TrueOrFalseForm],
    cyaWrites
  )
}
