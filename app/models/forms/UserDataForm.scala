package models.forms

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.nonEmptyText

final case class UserDataForm(username: String, password: String, returnUrl: String)

object UserDataForm {
  def unapply(u: UserDataForm): Option[(String, String, String)] = Some((u.username, u.password, u.returnUrl))

  val userForm: Form[UserDataForm] = Form(
    mapping(
      "username"  -> nonEmptyText,
      "password"  -> nonEmptyText,
      "returnUrl" -> nonEmptyText
    )(UserDataForm.apply)(UserDataForm.unapply)
  )
}
