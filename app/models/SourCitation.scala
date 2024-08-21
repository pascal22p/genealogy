package models

import java.time.Instant

import models.SourCitationType.SourCitationType

final case class SourCitation(
    id: Int,
    record: Option[SourRecord],
    page: String,
    even: String,
    role: String,
    dates: String,
    text: String,
    quay: Option[Int],
    subm: String,
    timestamp: Instant,
    ownerId: Option[Int],
    sourceType: SourCitationType,
    medias: List[Media] = List.empty
)

object SourCitation {
  def apply(sourCitationQueryData: SourCitationQueryData, medias: List[Media]): SourCitation = {
    new SourCitation(
      sourCitationQueryData.id,
      sourCitationQueryData.record,
      sourCitationQueryData.page,
      sourCitationQueryData.even,
      sourCitationQueryData.role,
      sourCitationQueryData.dates,
      sourCitationQueryData.text,
      sourCitationQueryData.quay,
      sourCitationQueryData.subm,
      sourCitationQueryData.timestamp,
      sourCitationQueryData.ownerId,
      sourCitationQueryData.sourceType,
      medias
    )
  }
}
