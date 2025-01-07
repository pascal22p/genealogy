package models

import java.time.Instant

import models.queryData.FamilyQueryData

final case class Family(
    id: Int,
    parent1: Option[Person],
    parent2: Option[Person],
    timestamp: Instant,
    privacyRestriction: Option[String],
    refn: String,
    children: List[Child] = List.empty,
    events: Events
)

object Family {
  def apply(familyQueryData: FamilyQueryData, parent1: Option[Person], parent2: Option[Person]): Family = {
    new Family(
      familyQueryData.id,
      parent1,
      parent2,
      familyQueryData.timestamp,
      familyQueryData.privacyRestriction,
      familyQueryData.refn,
      List.empty,
      Events(List.empty, Some(familyQueryData.id), EventType.FamilyEvent)
    )
  }
}
