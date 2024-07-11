package models

import anorm._
import anorm.SqlParser._
import queryData.FamilyAsChildQueryData

final case class Parents(family: Family, refnType: String, relaType: String, relaStat: Option[String])

object Parents {
  def apply(familyAsChildQueryData: FamilyAsChildQueryData, parent1: Option[Person], parent2: Option[Person]) = {
    new Parents(
      Family(familyAsChildQueryData.family, parent1, parent2),
      familyAsChildQueryData.refnType,
      familyAsChildQueryData.relaType,
      familyAsChildQueryData.relaStat
    )
  }
}
