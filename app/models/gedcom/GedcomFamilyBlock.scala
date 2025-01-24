package models.gedcom

import models.ResnType.ResnType

final case class GedcomFamilyBlock(
    id: Int,
    wife: Option[Int],
    husb: Option[Int],
    children: List[Int],
    events: List[GedcomEventBlock],
    resn: Option[ResnType]
)
