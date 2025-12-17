package models.forms

import play.api.data._
import play.api.data.Forms._

final case class GedcomListForm(selectedFile: String)

object GedcomListForm {
  def unapply(
      u: GedcomListForm
  ): Some[String] = Some(
    u.selectedFile
  )

  val form: Form[GedcomListForm] = Form(
    mapping(
      "selectedFile" -> nonEmptyText
    )(GedcomListForm.apply)(GedcomListForm.unapply)
  )
}
