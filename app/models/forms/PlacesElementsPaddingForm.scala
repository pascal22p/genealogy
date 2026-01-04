package models.forms

import models.journeyCache.UserAnswersItem
import play.api.data.validation.Constraint
import play.api.data.validation.Invalid
import play.api.data.validation.Valid
import play.api.data.validation.ValidationError
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.nonEmptyText
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.libs.json.OWrites

final case class PlacesElementsPaddingForm(padding: String) extends UserAnswersItem

object PlacesElementsPaddingForm {
  def unapply(
      u: PlacesElementsPaddingForm
  ): Some[String] = Some(
    u.padding
  )

  private val validPadding: Constraint[String] = Constraint("constraint.place.padding") { value =>
    if (value != "left" && value != "right") {
      Invalid(
        ValidationError(
          "Separator must be a single character"
        )
      )
    } else {
      Valid
    }
  }

  val form: Form[PlacesElementsPaddingForm] = Form(
    mapping(
      "padding" -> nonEmptyText.verifying(validPadding)
    )(PlacesElementsPaddingForm.apply)(PlacesElementsPaddingForm.unapply)
  )

  def cyaWrites(using messages: Messages): OWrites[PlacesElementsPaddingForm] =
    OWrites { form =>
      Json.obj(
        "padding" ->
          messages(s"importGedcom.PlacesElements.padding.${form.padding}")
      )
    }
}
