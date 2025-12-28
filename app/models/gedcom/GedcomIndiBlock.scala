package models.gedcom

import models.ResnType.ResnType

final case class GedcomIndiBlock(
    nameStructure: GedComPersonalNameStructure,
    resn: Option[ResnType],
    sex: String,
    id: Int,
    events: List[GedcomEventBlock],
    famcLinks: Set[Int],
    famsLinks: Set[Int]
)
