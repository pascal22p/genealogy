package models.forms

import java.time.Instant

import models.queryData.FamilyQueryData
import models.ResnType
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.data.Forms.optional
import play.api.data.Forms.text

final case class FamilyForm(
    base: Int,
    person1Id: Option[Int],
    person2Id: Option[Int],
    privacyRestriction: Option[ResnType.ResnType]
) {
  def familyQueryData: FamilyQueryData = {
    FamilyQueryData(
      0,
      person1Id,
      person2Id,
      Instant.now,
      privacyRestriction.map(_.toString),
      "",
      "",
      base
    )
  }

}

object FamilyForm {

  def unapply(
      u: FamilyForm
  ): Some[(Int, Option[Int], Option[Int], Option[String])] = Some(
    (u.base, u.person1Id, u.person2Id, u.privacyRestriction.map(_.toString))
  )

  private def applyFromForm(
      base: Int,
      person1Id: Option[Int],
      person2Id: Option[Int],
      privacyRestriction: Option[String]
  ): FamilyForm = {
    FamilyForm(
      base,
      person1Id,
      person2Id,
      privacyRestriction.flatMap(ResnType.fromString)
    )
  }

  val familyForm: Form[FamilyForm] = Form(
    mapping(
      "base"               -> number,
      "person1Id"          -> optional(number),
      "person2Id"          -> optional(number),
      "privacyRestriction" -> optional(text)
    )(FamilyForm.applyFromForm)(FamilyForm.unapply)
  )
}
