package models

import play.api.libs.json.OFormat
import play.api.libs.json.Json

final case class HistoryElement(personId: Int, name: String)

object HistoryElement {
  implicit val format: OFormat[HistoryElement] = Json.format[HistoryElement]
}

