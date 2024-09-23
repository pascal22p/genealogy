package models.forms

import anorm._
import anorm.SqlParser._
import play.api.data.format.Formats._
import play.api.data.FieldMapping
import play.api.data.Form
import play.api.data.Forms.ignored
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.data.Forms.{text, optional}
import models.Sex
import models.MaleSex
import models.PersonDetails
import java.time.Instant

final case class PersonDetailsForm(
    base: Int,
    id: Int,
    firstname: String,
    surname: String, // SURN
    sex: String,        // SEX
    firstnamePrefix: String,           // NPFX
    surnamePrefix: String,             // SPFX
    nameSuffix: String,                // NSFX
    nameGiven: String,                 // GIVN
    nameNickname: String,              // NICK
    privacyRestriction: Option[String] // RESN

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

  def unapply(u: PersonDetailsForm): Some[(Int, Int, String, String, String, String, String, String, String, String, Option[String])] = Some(
    (
      u.base,
      u.id,
      u.firstname,
      u.surname,
      u.sex,
      u.firstnamePrefix,
      u.surnamePrefix,
      u.nameSuffix,
      u.nameGiven,
      u.nameNickname,
      u.privacyRestriction
    )
  )

  val personDetailsForm: Form[PersonDetailsForm] = Form(
    mapping(
      "base"               -> number,
      "id"                 -> number,
      "firstname"          -> text,
      "surname"            -> text,
      "sex"                -> text.verifying(s => List("", "M", "F").contains(s)),
      "firstnamePrefix"    -> text,
      "surnamePrefix"      -> text,
      "nameSuffix"         -> text,
      "nameGiven"          -> text,
      "nameNickname"       -> text,
      "privacyRestriction" -> optional(text)
    )(PersonDetailsForm.apply)(PersonDetailsForm.unapply)
  )
}
