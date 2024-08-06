package queries

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.Future

import anorm.*
import anorm.SqlParser.*
import models.*
import models.queryData.*
import models.EventType.EventType
import models.EventType.FamilyEvent
import models.EventType.IndividualEvent
import models.EventType.UnknownEvent
import play.api.db.Database
import play.api.libs.json.Json

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

  def getEvents(id: Int, eventType: EventType): Future[List[EventDetailQueryData]] = Future {
    db.withConnection { implicit conn =>
      val where = eventType match {
        case IndividualEvent => "WHERE rel_indi_events.indi_id = {id}"
        case FamilyEvent     => "WHERE rel_familles_events.familles_id = {id}"
        case _               => "WHERE genea_events_details.events_details_id = {id}"
      }
      SQL(s"""SELECT genea_events_details.*, rel_indi_events.*, rel_familles_events.*,
             |       CASE
             |           WHEN rel_indi_events.indi_id IS NOT NULL THEN CONCAT(genea_individuals.indi_prenom, ' ', genea_individuals.indi_nom)
             |           WHEN rel_familles_events.familles_id IS NOT NULL THEN CONCAT(husb.indi_nom, ' - ', wife.indi_nom)
             |           ELSE NULL
             |       END AS description,
             |       CASE
             |           WHEN rel_indi_events.indi_id IS NOT NULL THEN rel_indi_events.events_tag
             |           WHEN rel_familles_events.familles_id IS NOT NULL THEN rel_familles_events.events_tag
             |           ELSE NULL
             |       END AS tag,
             |       CASE
             |           WHEN rel_indi_events.indi_id IS NOT NULL THEN "${IndividualEvent.toString}"
             |           WHEN rel_familles_events.familles_id IS NOT NULL THEN "${FamilyEvent.toString}"
             |           ELSE "${UnknownEvent.toString}"
             |       END AS event_type
             |
             |FROM `genea_events_details`
             |LEFT JOIN `rel_indi_events`
             |ON genea_events_details.events_details_id = rel_indi_events.events_details_id
             |LEFT JOIN genea_individuals
             |ON genea_individuals.indi_id = rel_indi_events.indi_id
             |LEFT JOIN `rel_familles_events`
             |ON genea_events_details.events_details_id = rel_familles_events.events_details_id
             |LEFT JOIN `genea_familles`
             |ON  genea_familles.familles_id = rel_familles_events.familles_id
             |LEFT JOIN `genea_individuals` AS husb
             |ON husb.indi_id = genea_familles.familles_husb
             |LEFT JOIN `genea_individuals` AS wife
             |ON wife.indi_id = genea_familles.familles_wife
             |LEFT JOIN (
             |    SELECT events_details_id , count(*) AS sourCount FROM rel_events_sources GROUP BY rel_events_sources.events_details_id
             | ) r ON r.events_details_id = genea_events_details.events_details_id
             |
             | $where""".stripMargin)
        .on("id" -> id)
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

  def getSurnamesList(id: Int)(implicit authenticatedRequest: AuthenticatedRequest[?]): Future[List[String]] = Future {
    db.withConnection { implicit conn =>
      val excludePrivate = "AND indi_resn IS NULL"
      val isExcluded = authenticatedRequest.localSession.sessionData.userData.fold(excludePrivate) { userData =>
        if (userData.seePrivacy) "" else excludePrivate
      }
      SQL(s"""SELECT indi_nom
             |FROM genea_individuals
             |WHERE base = {id} $isExcluded
             |GROUP BY indi_nom
             |ORDER BY indi_nom""".stripMargin)
        .on("id" -> id)
        .as(SqlParser.str("indi_nom").*)
    }
  }(databaseExecutionContext)

  def getFirstnamesList(id: Int, name: String)(
      implicit authenticatedRequest: AuthenticatedRequest[?]
  ): Future[List[PersonDetails]] = Future {
    db.withConnection { implicit conn =>
      val excludePrivate = "AND indi_resn IS NULL"
      val isExcluded = authenticatedRequest.localSession.sessionData.userData.fold(excludePrivate) { userData =>
        if (userData.seePrivacy) "" else excludePrivate
      }
      SQL(s"""SELECT *
             |FROM genea_individuals
             |WHERE base = {id} AND indi_nom = {name} $isExcluded
             |ORDER BY indi_prenom""".stripMargin)
        .on("id" -> id, "name" -> name)
        .as(PersonDetails.mysqlParser.*)
    }
  }(databaseExecutionContext)

  def getSessionData(sessionId: String): Future[Option[Session]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM genea_sessions
            |WHERE sessionId = {id}""".stripMargin)
        .on("id" -> sessionId)
        .as(Session.mysqlParser.singleOpt)
    }
  }(databaseExecutionContext)

  def putSessionData(session: Session): Future[Option[String]] = Future {
    db.withConnection { implicit conn =>
      SQL("""INSERT INTO genea_sessions (sessionId, sessionData)
            |VALUES ({id}, {data})
            |""".stripMargin)
        .on("id" -> session.sessionId, "data" -> Json.toJson(session.sessionData).toString)
        .executeInsert(str(1).singleOpt)
    }
  }(databaseExecutionContext)

  def updateSessionData(session: Session): Future[Int] = Future {
    db.withConnection { implicit conn =>
      SQL("""UPDATE genea_sessions
            |SET sessionData = {data}
            |WHERE sessionId = {id}
            |""".stripMargin)
        .on("id" -> session.sessionId, "data" -> Json.toJson(session.sessionData).toString)
        .executeUpdate()
    }
  }(databaseExecutionContext)

  def sessionKeepAlive(sessionId: String): Future[Int] = Future {
    db.withConnection { implicit conn =>
      SQL("""UPDATE genea_sessions
            |SET timeStamp = CURRENT_TIMESTAMP
            |WHERE sessionId = {id}""".stripMargin)
        .on("id" -> sessionId)
        .executeUpdate()
    }
  }(databaseExecutionContext)

  def getUserData(username: String): Future[Option[UserData]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM genea_membres
            |WHERE email = {email}""".stripMargin)
        .on("email" -> username)
        .as(UserData.mysqlParser.singleOpt)
    }
  }(databaseExecutionContext)

}
