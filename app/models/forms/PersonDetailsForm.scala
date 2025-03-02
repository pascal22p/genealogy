package models.forms

import java.time.Instant

import models.PersonDetails
import models.ResnType
import models.ResnType._
import models.Sex
import play.api.data.format.Formats._
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.data.Forms.optional
import play.api.data.Forms.text

final case class PersonDetailsForm(
    base: Int,
    id: Int,
    firstname: String,
    surname: String,                              // SURN
    sex: String,                                  // SEX
    firstnamePrefix: String,                      // NPFX
    surnamePrefix: String,                        // SPFX
    nameSuffix: String,                           // NSFX
    nameGiven: String,                            // GIVN
    nameNickname: String,                         // NICK
    privacyRestriction: Option[ResnType.ResnType] // RESN

) {
  def toPersonalDetails: PersonDetails = {
    PersonDetails(
      base,
      id,
      firstname,
      surname,
      Sex.fromString(sex),
      Instant.now,
      firstnamePrefix,
      surnamePrefix,
      nameSuffix,
      nameGiven,
      nameNickname,
      privacyRestriction
    )
  }

}

object PersonDetailsForm {

  def unapply(
      u: PersonDetailsForm
  ): Some[(Int, Option[Int], String, String, String, String, String, String, String, String, Option[String])] = Some(
    (
      u.base,
      if (u.id == 0) None else Some(u.id),
      u.firstname,
      u.surname,
      u.sex,
      u.firstnamePrefix,
      u.surnamePrefix,
      u.nameSuffix,
      u.nameGiven,
      u.nameNickname,
      u.privacyRestriction.map(_.toString)
    )
  )

  def apply(
      base: Int,
      id: Option[Int],
      firstname: String,
      surname: String,
      sex: String,
      firstnamePrefix: String,
      surnamePrefix: String,
      nameSuffix: String,
      nameGiven: String,
      nameNickname: String,
      privacyRestriction: Option[String]
  ): PersonDetailsForm = {
    PersonDetailsForm(
      base,
      id.getOrElse(0),
      firstname,
      surname,
      sex,
      firstnamePrefix,
      surnamePrefix,
      nameSuffix,
      nameGiven,
      nameNickname,
      privacyRestriction.flatMap(ResnType.fromString)
    )
  }

  val personDetailsForm: Form[PersonDetailsForm] = Form(
    mapping(
      "base"               -> number,
      "id"                 -> optional(number),
      "firstname"          -> text,
      "surname"            -> text,
      "sex"                -> text.verifying("Value is not Unknown, Male or Female", s => List("", "M", "F").contains(s)),
      "firstnamePrefix"    -> text,
      "surnamePrefix"      -> text,
      "nameSuffix"         -> text,
      "nameGiven"          -> text,
      "nameNickname"       -> text,
      "privacyRestriction" -> optional(text)
    )(PersonDetailsForm.apply)(PersonDetailsForm.unapply)
  )
}
