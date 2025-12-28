package models.forms

import scala.annotation.unused

import models.journeyCache.UserAnswersItem
import play.api.data.Form
import play.api.data.Forms.single
import play.api.data.Forms.text
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.libs.json.OWrites

final case class SelectExistingDatabaseForm(id: Int, title: String) extends UserAnswersItem

object SelectExistingDatabaseForm {
  def cyaWrites(using @unused messages: Messages): OWrites[SelectExistingDatabaseForm] =
    OWrites { form =>
      Json.obj(
        "selectedDatabase" ->
          s"${form.title} (id: ${form.id})"
      )
    }

  def unapply(
      u: SelectExistingDatabaseForm
  ): Some[(Int, String)] = Some(
    (u.id, u.title)
  )

  val form: Form[SelectExistingDatabaseForm] = Form(
    single(
      "selectedDatabase" -> text.transform[SelectExistingDatabaseForm](
        str => {
          val Array(id, title) = str.split("\\|", 2)
          SelectExistingDatabaseForm(id.toInt, title)
        },
        form => s"${form.id}|${form.title}"
      )
    )
  )
}
