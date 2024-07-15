package models

import anorm.*
import anorm.SqlParser.*
import play.api.libs.json.Json
import play.api.libs.json.OFormat

case class UserData(id: Int, username: String, hashedPassword: String, seePrivacy: Boolean, isAdmin: Boolean)

object UserData {
  implicit val format: OFormat[UserData] = Json.format[UserData]

  val mysqlParser: RowParser[UserData] =
    (get[Int]("id") ~
      get[String]("email") ~
      get[String]("saltpass") ~
      get[Int]("see_privacy") ~
      get[Int]("is_admin")).map {
      case id ~ username ~ hashedPassword ~ seePrivacy ~ isAdmin =>
        val seePrivacyBool = if (seePrivacy == 1) true else false
        val isAdminBool    = if (isAdmin == 1) true else false
        UserData(id, username, hashedPassword, seePrivacyBool, isAdminBool)
    }
}
