package models

import play.api.i18n.Messages

final case class Person(
    details: PersonDetails,
    events: Events,
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

  def nameBirthDeathAndRelations(implicit messages: Messages): String = {
    val partners: List[Person] = families.flatMap { family =>
      findPartner(family.id)
    }
    s"${details.firstname} ${details.surname} " +
      events.birthAndDeathDate +
      " x " + partners.map(person => s"${person.details.firstname} ${person.details.surname} ").mkString(" x ")
  }

  def name(implicit messages: Messages): String = {
    val partners: List[Person] = families.flatMap { family =>
      findPartner(family.id)
    }
    s"${details.firstname} ${details.surname}"
  }

}
