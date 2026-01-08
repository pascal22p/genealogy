package models.gedcom

import play.api.libs.json.Format
import play.api.libs.json.Json

final case class GedcomObject(nodes: Seq[GedcomNode])

object GedcomObject {
  implicit val format: Format[GedcomObject] = Json.format[GedcomObject]
}
