package models.queryData

import anorm.~
import anorm.RowParser
import anorm.SqlParser.get
import config.AppConfig
import models.AuthenticatedRequest
import play.api.i18n.Messages
import utils.GedcomDateLibrary

final case class FirstnameWithBirthDeath(
    id: Int,
    firstname: String,
    birth: Option[String],
    death: Option[String],
    birthJd: Int,
    deathJd: Int
) {
  def birthAndDeathDate(
      implicit messages: Messages,
      authenticatedRequest: AuthenticatedRequest[?],
      appConfig: AppConfig
  ): String = {
    GedcomDateLibrary.birthAndDeathDate(birthDate = birth, deathDate = death, shortMonth = false)
  }
}

object FirstnameWithBirthDeath {
  val mysqlParser: RowParser[FirstnameWithBirthDeath] =
    (get[Int]("indi_id") ~
      get[String]("indi_prenom") ~
      get[Option[String]]("birth_date") ~
      get[Option[String]]("death_date") ~
      get[Option[Int]]("birth_date_jd") ~
      get[Option[Int]]("death_date_jd")).map {
      case id ~ firstname ~ birth ~ death ~ birthJd ~ deathJd =>
        FirstnameWithBirthDeath(
          id,
          firstname,
          birth.filter(_.nonEmpty),
          death.filter(_.nonEmpty),
          birthJd.getOrElse(0),
          deathJd.getOrElse(0)
        )
    }
}
