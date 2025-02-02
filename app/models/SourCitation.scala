package models

import java.time.Instant

import models.forms.SourCitationForm
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
) {
  def toForm: SourCitationForm =
    SourCitationForm(
      dates,
      even,
      role,
      subm,
      text,
      page,
      quay,
      record.map(_.id)
    )

  def fromForm(sourCitationForm: SourCitationForm): SourCitation = SourCitation(
    id,
    sourCitationForm.recordId.map(id => SourRecord(id, "", "", "", "", "", "", None, "", "", Instant.now())),
    sourCitationForm.page,
    sourCitationForm.even,
    sourCitationForm.role,
    sourCitationForm.date,
    sourCitationForm.text,
    sourCitationForm.quay,
    sourCitationForm.submitter,
    timestamp,
    ownerId,
    sourceType
  )
}

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
