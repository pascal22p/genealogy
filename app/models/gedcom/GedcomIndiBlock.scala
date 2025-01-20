package models.gedcom

import models.ResnType.ResnType

final case class GedcomIndiBlock(
    nameStructure: GedComPersonalNameStructure,
    resn: Option[ResnType],
    sex: String,
    id: Int
)
