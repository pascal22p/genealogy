package models.forms

import models.journeyCache.UserAnswersItem
import play.api.data.validation.Constraint
import play.api.data.validation.Invalid
import play.api.data.validation.Valid
import play.api.data.validation.ValidationError
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.nonEmptyText

final case class PlacesElementsSeparatorForm(separator: String) extends UserAnswersItem

object PlacesElementsSeparatorForm {
  def unapply(
      u: PlacesElementsSeparatorForm
  ): Some[String] = Some(
    u.separator
  )

  private val validSeparator: Constraint[String] = Constraint("constraint.place.separator") { value =>
    if (value.trim.length > 1) {
      Invalid(
        ValidationError(
          "Separator must be a single character"
        )
      )
    } else {
      Valid
    }
  }

  val form: Form[PlacesElementsSeparatorForm] = Form(
    mapping(
      "separator" -> nonEmptyText.verifying(validSeparator)
    )(PlacesElementsSeparatorForm.apply)(PlacesElementsSeparatorForm.unapply)
  )

}
