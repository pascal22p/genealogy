package models.forms

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.nonEmptyText

case class UserDataForm(username: String, password: String)
object UserDataForm {
  def unapply(u: UserDataForm): Option[(String, String)] = Some((u.username, u.password))

  val userForm: Form[UserDataForm] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(UserDataForm.apply)(UserDataForm.unapply)
  )
}
