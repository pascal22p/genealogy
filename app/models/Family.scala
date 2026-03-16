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
  def formatPartnerName: String = {
    val partner = parent1.orElse(parent2)
    partner.map(p => s"${p.details.firstname} ${p.details.surname}").getOrElse("Unknown")
  }

  def formatFamilyName: String = {
    (parent1, parent2) match {
      case (None, None)         => "Unknown Family"
      case (Some(p1), None)     => p1.details.surname
      case (None, Some(p2))     => p2.details.surname
      case (Some(p1), Some(p2)) => s"${p1.details.surname} — ${p2.details.surname}"
    }
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
