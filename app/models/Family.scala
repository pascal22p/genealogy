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
) {
  def formatPartnerName(personId: Int): String = {
    val partner = if (parent1.exists(_.details.id == personId)) parent2 else parent1
    partner.map(p => s"${p.details.firstname} ${p.details.surname}").getOrElse("Unknown")
  }
}

object Family {
  def apply(
      familyQueryData: FamilyQueryData,
      parent1: Option[Person],
      parent2: Option[Person],
      children: List[Child],
      events: List[EventDetail]
  ): Family = {
    new Family(
      familyQueryData.id,
      parent1,
      parent2,
      familyQueryData.timestamp,
      familyQueryData.privacyRestriction,
      familyQueryData.refn,
      children,
      Events(events, Some(familyQueryData.id), EventType.FamilyEvent)
    )
  }
}
