package models.gedcom

import play.api.libs.json.Format
import play.api.libs.json.Json

final case class GedcomObject(nodes: List[GedcomNode])

object GedcomObject {
  implicit val format: Format[GedcomObject] = Json.format[GedcomObject]
}
