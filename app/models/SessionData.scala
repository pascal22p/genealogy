package models

import anorm.*
import anorm.SqlParser.*
import play.api.libs.json.Json
import play.api.libs.json.OFormat

case class SessionData(dbId: Int, userData: Option[UserData] = None)

object SessionData {
  implicit val format: OFormat[SessionData] = Json.format[SessionData]
}

case class Session(sessionId: String, sessionData: SessionData)

object Session {
  val mysqlParser: RowParser[Session] =
    (get[String]("sessionId") ~
      get[String]("sessionData")).map {
      case id ~ sessionData =>
        Session(id, Json.parse(sessionData).as[SessionData])
    }
}
