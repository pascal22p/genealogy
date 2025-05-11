package models.forms

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.number

final case class LinkForm(
    linkId: Int
)

object LinkForm {

  def unapply(
      u: LinkForm
  ): Some[Int] = Some(
    u.linkId
  )

  val linkForm: Form[LinkForm] = Form(
    mapping(
      "linkId" -> number
    )(LinkForm.apply)(LinkForm.unapply)
  )
}
