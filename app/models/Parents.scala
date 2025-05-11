package models

import queryData.FamilyAsChildQueryData

final case class Parents(family: Family, refnType: String, relaType: String, relaStat: Option[String]) {
  def formatParentsNames: String = {
    val parent1Name = family.parent1.map(p => s"${p.details.firstname} ${p.details.surname}").getOrElse("Unknown")
    val parent2Name = family.parent2.map(p => s"${p.details.firstname} ${p.details.surname}").getOrElse("Unknown")
    s"$parent1Name and $parent2Name"
  }
}

object Parents {
  def apply(familyAsChildQueryData: FamilyAsChildQueryData, parent1: Option[Person], parent2: Option[Person]) = {
    new Parents(
      Family(familyAsChildQueryData.family, parent1, parent2, List.empty, List.empty),
      familyAsChildQueryData.refnType,
      familyAsChildQueryData.relaType,
      familyAsChildQueryData.relaStat
    )
  }
}
