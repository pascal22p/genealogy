package models.gedcom

import play.api.libs.json.Format
import play.api.libs.json.Json

final case class GedcomNode(
    name: String,
    line: String,
    lineNumber: Int,
    level: Int,
    xref: Option[String],
    content: Option[String],
    children: Seq[GedcomNode]
)

object GedcomNode {
  implicit val format: Format[GedcomNode] = Json.format[GedcomNode]
}
