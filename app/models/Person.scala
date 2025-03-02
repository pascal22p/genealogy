package models

final case class Person(
    details: PersonDetails,
    events: Events,
    attributes: Attributes,
    parents: List[Parents] = List.empty,
    families: List[Family] = List.empty
) {
  def findPartner(familyId: Int): Option[Person] = {
    families.find(_.id == familyId).flatMap { family =>
      if (family.parent1.exists(_.details.id == details.id)) {
        family.parent2
      } else if (family.parent2.exists(_.details.id == details.id)) {
        family.parent1
      } else {
        None
      }
    }
  }

  def name: String = {
    s"${details.firstname} ${details.surname}"
  }

}
