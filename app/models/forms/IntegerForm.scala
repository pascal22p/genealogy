package models.forms

import scala.annotation.unused

import models.journeyCache.UserAnswersItem
import play.api.data.Form
import play.api.data.Forms.single
import play.api.data.Forms.text
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.libs.json.OWrites

final case class IntegerForm(
    number: Int,
    label: String
) extends UserAnswersItem

object IntegerForm {

  def unapply(
      u: IntegerForm
  ): Some[(Int, String)] = Some(
    (u.number, u.label)
  )

  val integerFormWithLabel: Form[IntegerForm] = Form(
    single(
      "integerWithLabel" -> text.transform[IntegerForm](
        str => {
          val Array(number, label) = str.split("\\|", 2)
          IntegerForm(number.toInt, label)
        },
        form => s"${form.number}|${form.label}"
      )
    )
  )

  def cyaWrites(using @unused messages: Messages): OWrites[IntegerForm] =
    OWrites { form =>
      Json.obj(
        "integerLabel" ->
          form.label
      )
    }
}
