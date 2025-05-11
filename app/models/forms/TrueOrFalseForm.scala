package models.forms

import play.api.data.Form
import play.api.data.Forms.boolean
import play.api.data.Forms.mapping

final case class TrueOrFalseForm(
    trueOrFalse: Boolean
)

object TrueOrFalseForm {

  def unapply(
      u: TrueOrFalseForm
  ): Some[Boolean] = Some(
    u.trueOrFalse
  )

  val trueOrFalseForm: Form[TrueOrFalseForm] = Form(
    mapping(
      "trueOrFalse" -> boolean
    )(TrueOrFalseForm.apply)(TrueOrFalseForm.unapply)
  )
}
