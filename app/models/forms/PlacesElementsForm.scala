package models.forms

import models.journeyCache.UserAnswersItem
import play.api.data.validation.Constraint
import play.api.data.validation.Invalid
import play.api.data.validation.Valid
import play.api.data.validation.ValidationError
import play.api.data.Form
import play.api.data.Forms.list
import play.api.data.Forms.mapping
import play.api.data.Forms.text

final case class PlacesElementsForm(hierarchy: List[String]) extends UserAnswersItem

object PlacesElementsForm {
  def unapply(
      u: PlacesElementsForm
  ): Some[List[String]] = Some(
    u.hierarchy
  )

  private val uniqueNonEmpty: Constraint[List[String]] = Constraint("constraint.unique") { values =>
    val cleaned    = values.map(_.trim).filter(_.nonEmpty)
    val duplicates = cleaned.diff(cleaned.distinct).distinct

    if (duplicates.isEmpty) {
      Valid
    } else {
      Invalid(
        ValidationError(
          "Duplicate values found: " + duplicates.mkString(", ")
        )
      )
    }
  }

  val form: Form[PlacesElementsForm] = Form(
    mapping(
      "hierarchy" -> list(text).verifying(uniqueNonEmpty)
    )(PlacesElementsForm.apply)(PlacesElementsForm.unapply)
  )

}
