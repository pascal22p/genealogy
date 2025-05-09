package models

import java.time.Instant

import anorm._
import anorm.SqlParser._

final case class Child(person: Person, relaType: String, relaStat: Option[String])

object Child {
  val mysqlParser: RowParser[Child] =
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
      get[Option[String]]("indi_resn") ~
      get[String]("rela_type") ~
      get[Option[String]]("rela_stat")).map {
      case id ~ base ~ surname ~ firstname ~ sex ~ timestamp ~ firstnamePrefix ~
          nameGiven ~ nameNickname ~ surnamePrefix ~ nameSuffix ~ resn ~ relaType ~ relaStat =>
        Child(
          Person(
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
              resn.flatMap(ResnType.fromString)
            ),
            Events(List.empty, Some(id), EventType.IndividualEvent),
            Attributes(List.empty, Some(id), EventType.IndividualEvent)
          ),
          relaType,
          relaStat
        )
    }
}
