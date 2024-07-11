package queries

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.Future

import anorm._
import anorm.SqlParser._
import models._
import models.queryData.EventDetailQueryData
import models.queryData.FamilyAsChildQueryData
import models.queryData.FamilyQueryData
import models.PersonDetails
import play.api.db.Database

@Singleton
final class MariadbQueries @Inject() (db: Database, databaseExecutionContext: DatabaseExecutionContext) {

  def getPersonDetails(id: Int): Future[List[PersonDetails]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM genea_individuals
            |WHERE indi_id = {id}""".stripMargin)
        .on("id" -> id)
        .as[List[PersonDetails]](PersonDetails.mysqlParser.*)
    }
  }(databaseExecutionContext)

  def getIndividualEvents(personId: Int): Future[List[EventDetailQueryData]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT rel_indi_events.events_tag, genea_events_details.*
            |FROM `rel_indi_events`
            |LEFT JOIN genea_events_details
            |ON genea_events_details.events_details_id = rel_indi_events.events_details_id
            |WHERE indi_id = {id}""".stripMargin)
        .on("id" -> personId)
        .as[List[EventDetailQueryData]](EventDetailQueryData.mysqlParser.*)
    }
  }(databaseExecutionContext)

  def getFamilyEvents(familyId: Int): Future[List[EventDetailQueryData]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT rel_familles_events.events_tag, genea_events_details.*
            |FROM `rel_familles_events`
            |LEFT JOIN genea_events_details
            |ON genea_events_details.events_details_id = rel_familles_events.events_details_id
            |WHERE familles_id = {id}""".stripMargin)
        .on("id" -> familyId)
        .as[List[EventDetailQueryData]](EventDetailQueryData.mysqlParser.*)
    }
  }(databaseExecutionContext)

  def getFamiliesFromIndividualId(individualId: Int): Future[List[FamilyAsChildQueryData]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM rel_familles_indi
            |LEFT JOIN genea_familles
            |ON genea_familles.familles_id = rel_familles_indi.familles_id
            |WHERE indi_id = {id}""".stripMargin)
        .on("id" -> individualId)
        .as[List[FamilyAsChildQueryData]](FamilyAsChildQueryData.mysqlParser.*)
    }
  }(databaseExecutionContext)

  def getFamiliesAsPartner(individualId: Int): Future[List[FamilyQueryData]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM genea_familles
            |WHERE familles_husb = {id} OR familles_wife = {id}""".stripMargin)
        .on("id" -> individualId)
        .as[List[FamilyQueryData]](FamilyQueryData.mysqlParser.*)
    }
  }(databaseExecutionContext)

  def getFamilyDetails(familyId: Int): Future[Option[FamilyQueryData]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM genea_familles
            |WHERE familles_id = {id}""".stripMargin)
        .on("id" -> familyId)
        .as[Option[FamilyQueryData]](FamilyQueryData.mysqlParser.singleOpt)
    }
  }(databaseExecutionContext)

  def getChildren(familyId: Int): Future[List[Child]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM rel_familles_indi
            |LEFT JOIN genea_individuals
            |ON genea_individuals.indi_id = rel_familles_indi.indi_id
            |WHERE rel_familles_indi.familles_id = {id}""".stripMargin)
        .on("id" -> familyId)
        .as[List[Child]](Child.mysqlParser.*)
    }
  }(databaseExecutionContext)

  def getPlace(id: Int): Future[Option[Place]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM genea_place
            |WHERE place_id = {id}""".stripMargin)
        .on("id" -> id)
        .as[Option[Place]](Place.mysqlParser.singleOpt)
    }
  }(databaseExecutionContext)

  def getGenealogyDatabases: Future[List[GenealogyDatabase]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT id, nom, descriptif
            |FROM genea_infos
            |ORDER BY nom""".stripMargin)
        .as[List[GenealogyDatabase]](GenealogyDatabase.mysqlParser.*)
    }
  }(databaseExecutionContext)

  def getSurnamesList(id: Int): Future[List[String]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT indi_nom
            |FROM genea_individuals
            |WHERE base = {id} AND indi_resn IS NULL
            |GROUP BY indi_nom
            |ORDER BY indi_nom""".stripMargin)
        .on("id" -> id)
        .as(SqlParser.str("indi_nom").*)
    }
  }(databaseExecutionContext)

  def getFirstnamesList(id: Int, name: String): Future[List[PersonDetails]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM genea_individuals
            |WHERE base = {id} AND indi_nom = {name} AND indi_resn IS NULL
            |ORDER BY indi_prenom""".stripMargin)
        .on("id" -> id, "name" -> name)
        .as(PersonDetails.mysqlParser.*)
    }
  }(databaseExecutionContext)

}
