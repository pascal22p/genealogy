package models.gedcom

final case class GedcomNode(
    name: String,
    line: String,
    lineNumber: Int,
    level: Int,
    xref: Option[String],
    content: Option[String],
    children: List[GedcomNode]
)

final case class GedcomTree(root: List[GedcomNode])
