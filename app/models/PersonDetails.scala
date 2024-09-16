package models

import java.time.Instant

import anorm._
import anorm.SqlParser._

final case class PersonDetails(
    base: Int,
    id: Int,
    firstname: String,
    surname: String, // SURN
    sex: Sex,        // SEX
    timestamp: Instant,
    firstnamePrefix: String,           // NPFX
    surnamePrefix: String,             // SPFX
    nameSuffix: String,                // NSFX
    nameGiven: String,                 // GIVN
    nameNickname: String,              // NICK
    privacyRestriction: Option[String] // RESN

) {
  def shortName: String = (firstname + " " + surname).trim
}

object PersonDetails {
  val mysqlParser: RowParser[PersonDetails] =
    (get[Int]("indi_id") ~
      get[Int]("base") ~
      get[String]("indi_nom") ~
      get[String]("indi_prenom") ~
      get[String]("indi_sexe") ~
      get[Option[Instant]]("indi_timestamp") ~
      get[String]("indi_npfx") ~
      get[String]("indi_givn") ~
      get[String]("indi_nick") ~
      get[String]("indi_spfx") ~
      get[String]("indi_nsfx") ~
      get[Option[String]]("indi_resn")).map {
      case id ~ base ~ surname ~ firstname ~ sex ~ timestamp ~ firstnamePrefix ~
          nameGiven ~ nameNickname ~ surnamePrefix ~ nameSuffix ~ resn =>
        PersonDetails(
          base,
          id,
          firstname,
          surname,
          Sex.fromString(sex),
          timestamp.getOrElse(Instant.now),
          firstnamePrefix,
          surnamePrefix,
          nameSuffix,
          nameGiven,
          nameNickname,
          resn
        )
    }
}
