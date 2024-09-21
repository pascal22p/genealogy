package models

import anorm.*
import anorm.SqlParser.*
import play.api.libs.json.Json
import play.api.libs.json.OFormat

final case class SessionData(dbId: Int, userData: Option[UserData] = None, history: List[HistoryElement] = List.empty)

object SessionData {
  implicit val format: OFormat[SessionData] = Json.format[SessionData]
}
