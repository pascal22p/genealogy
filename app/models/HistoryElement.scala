package models

import play.api.libs.json.Json
import play.api.libs.json.OFormat

final case class HistoryElement(personId: Int, name: String)

object HistoryElement {
  implicit val format: OFormat[HistoryElement] = Json.format[HistoryElement]
}
