package models.forms

import models.journeyCache.UserAnswersItem
import play.api.data.Form
import play.api.data.Forms.list
import play.api.data.Forms.mapping
import play.api.data.Forms.text
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.libs.json.OWrites

final case class PlacesElementsForm(hierarchy: List[String]) extends UserAnswersItem

object PlacesElementsForm {
  def unapply(
      u: PlacesElementsForm
  ): Some[List[String]] = Some(
    u.hierarchy
  )

  val form: Form[PlacesElementsForm] = Form(
    mapping(
      "hierarchy" -> list(text)
    )(PlacesElementsForm.apply)(PlacesElementsForm.unapply)
  )

  def cyaWrites(using messages: Messages): OWrites[PlacesElementsForm] =
    OWrites { form =>
      Json.obj(
        "hierarchy" ->
          form.hierarchy.map(el => messages(s"importGedcom.place.field.$el")).mkString(", ")
      )
    }

}
