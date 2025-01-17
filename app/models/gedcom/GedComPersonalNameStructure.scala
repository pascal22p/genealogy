package models.gedcom

final case class GedComPersonalNameStructure(
    name: String,
    npfx: String,
    givn: String,
    nick: String,
    spfx: String,
    nsfx: String,
    surn: String
)
