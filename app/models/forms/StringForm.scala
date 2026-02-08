package models.forms

import models.journeyCache.UserAnswersItem
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.text

final case class StringForm(
    value: String
) extends UserAnswersItem

object StringForm {

  def unapply(
      u: StringForm
  ): Some[String] = Some(
    u.value
  )

  val stringForm: Form[StringForm] = Form(
    mapping(
      "value" -> text
    )(StringForm.apply)(StringForm.unapply)
  )
}
