package models.gedcom

import models.Family
import models.Person

final case class GedcomObject(individuals: List[Person], families: List[Family])
