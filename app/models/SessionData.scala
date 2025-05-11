package models

import play.api.libs.json.Json
import play.api.libs.json.OFormat

final case class SessionData(userData: Option[UserData] = None, history: List[HistoryElement] = List.empty)

object SessionData {
  implicit val format: OFormat[SessionData] = Json.format[SessionData]
}
