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
import models.MediaType.EventMedia
import models.MediaType.FamilyMedia
import models.MediaType.IndividualMedia
import models.MediaType.MediaType
import models.MediaType.SourCitationMedia
import models.MediaType.UnknownMedia
import models.SourCitationType.EventSourCitation
import models.SourCitationType.FamilySourCitation
import models.SourCitationType.IndividualSourCitation
import models.SourCitationType.SourCitationType
import models.SourCitationType.UnknownSourCitation
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

  def updatePersonDetails(personDetails: PersonDetails): Future[Int] = Future {
    db.withConnection { implicit conn =>
      SQL("""UPDATE genea_individuals
            |SET base = {base},
            |indi_nom = {surname},
            |indi_prenom = {firstname},
            |indi_sexe = {sex},
            |indi_timestamp = {timestamp},
            |indi_npfx = {npfx},
            |indi_givn = {nameGiven},
            |indi_nick = {nickname},
            |indi_spfx = {spfx},
            |indi_nsfx = {nsfx},
            |indi_resn = {resn}
            |WHERE indi_id = {id}""".stripMargin)
        .on(
          "base"      -> personDetails.base,
          "surname"   -> personDetails.surname,
          "firstname" -> personDetails.firstname,
          "sex"       -> personDetails.sex.gedcom,
          "timestamp" -> personDetails.timestamp,
          "npfx"      -> personDetails.firstnamePrefix,
          "nameGiven" -> personDetails.nameGiven,
          "nickname"  -> personDetails.nameNickname,
          "spfx"      -> personDetails.surnamePrefix,
          "nsfx"      -> personDetails.nameSuffix,
          "resn"      -> personDetails.privacyRestriction,
          "id"        -> personDetails.id
        )
        .executeUpdate()
    }
  }(databaseExecutionContext)

  def getEvents(id: Int, eventType: EventType): Future[List[EventDetailQueryData]] = Future {
    db.withConnection { implicit conn =>
      val where = eventType match {
        case IndividualEvent => "WHERE rel_indi_events.indi_id = {id}"
        case FamilyEvent     => "WHERE rel_familles_events.familles_id = {id}"
        case _               => "WHERE genea_events_details.events_details_id = {id}"
      }
      SQL(s"""SELECT genea_events_details.*, rel_indi_events.*, rel_familles_events.*, r.sourCount,
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
             |       END AS event_type,
             |       CASE
             |           WHEN rel_indi_events.indi_id IS NOT NULL THEN rel_indi_events.indi_id
             |           WHEN rel_familles_events.familles_id IS NOT NULL THEN rel_familles_events.familles_id
             |           ELSE NULL
             |       END AS ownerId,
             |       CASE
             |           WHEN rel_indi_events.indi_id IS NOT NULL THEN genea_individuals.indi_resn
             |           WHEN rel_familles_events.familles_id IS NOT NULL THEN genea_familles.familles_resn
             |           ELSE NULL
             |       END AS resn
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

  def getSourCitations(id: Int, typeCitation: SourCitationType): Future[List[SourCitationQueryData]] = Future {
    db.withConnection { implicit conn =>
      val where = typeCitation match {
        case EventSourCitation      => "WHERE rel_events_sources.events_details_id = {id}"
        case IndividualSourCitation => "WHERE rel_indi_sources.indi_id = {id}"
        case FamilySourCitation     => "WHERE rel_familles_sources.familles_id = {id}"
        case UnknownSourCitation    => "WHERE genea_sour_citations.sour_citations_id = {id}"
      }

      SQL(s"""SELECT *,
             |       CASE
             |           WHEN rel_events_sources.events_details_id IS NOT NULL THEN "${EventSourCitation.toString}"
             |           WHEN rel_indi_sources.indi_id IS NOT NULL THEN "${IndividualSourCitation.toString}"
             |           WHEN rel_familles_sources.familles_id IS NOT NULL THEN "${FamilySourCitation.toString}"
             |           ELSE "${UnknownSourCitation.toString}"
             |       END AS source_type,
             |       CASE
             |           WHEN rel_events_sources.events_details_id IS NOT NULL THEN rel_events_sources.events_details_id
             |           WHEN rel_indi_sources.indi_id IS NOT NULL THEN rel_indi_sources.indi_id
             |           WHEN rel_familles_sources.familles_id IS NOT NULL THEN rel_familles_sources.familles_id
             |           ELSE NULL
             |       END AS owner_id
             |
             |FROM genea_sour_citations
             |LEFT JOIN genea_sour_records ON genea_sour_records.sour_records_id = genea_sour_citations.sour_records_id
             |LEFT JOIN rel_events_sources ON rel_events_sources.sour_citations_id = genea_sour_citations.sour_citations_id
             |LEFT JOIN rel_indi_sources ON rel_indi_sources.sour_citations_id = genea_sour_citations.sour_citations_id
             |LEFT JOIN rel_familles_sources ON rel_familles_sources.sour_citations_id = genea_sour_citations.sour_citations_id
             |$where""".stripMargin)
        .on("id" -> id)
        .as[List[SourCitationQueryData]](SourCitationQueryData.mysqlParser.*)
    }
  }(databaseExecutionContext)

  def getMedias(id: Int, typeMedia: MediaType): Future[List[Media]] = Future {
    db.withConnection { implicit conn =>
      val where = typeMedia match {
        case EventMedia        => "WHERE rel_events_multimedia.events_details_id = {id}"
        case IndividualMedia   => "WHERE rel_indi_multimedia.indi_id = {id}"
        case FamilyMedia       => "WHERE rel_familles_multimedia.familles_id = {id}"
        case SourCitationMedia => "WHERE rel_sour_citations_multimedia.sour_citations_id = {id}"
        case UnknownMedia      => "WHERE genea_multimedia.media_id = {id}"
      }

      SQL(
        s"""SELECT *,
           |       CASE
           |           WHEN rel_events_multimedia.events_details_id IS NOT NULL THEN "${EventMedia.toString}"
           |           WHEN rel_indi_multimedia.indi_id IS NOT NULL THEN "${IndividualMedia.toString}"
           |           WHEN rel_familles_multimedia.familles_id IS NOT NULL THEN "${FamilyMedia.toString}"
           |           WHEN rel_sour_citations_multimedia.sour_citations_id IS NOT NULL THEN "${SourCitationMedia.toString}"
           |           ELSE "${UnknownMedia.toString}"
           |       END AS media_type,
           |       CASE
           |           WHEN rel_events_multimedia.events_details_id IS NOT NULL THEN rel_events_multimedia.events_details_id
           |           WHEN rel_indi_multimedia.indi_id IS NOT NULL THEN rel_indi_multimedia.indi_id
           |           WHEN rel_familles_multimedia.familles_id IS NOT NULL THEN rel_familles_multimedia.familles_id
           |           WHEN rel_sour_citations_multimedia.sour_citations_id IS NOT NULL THEN rel_sour_citations_multimedia.sour_citations_id
           |           ELSE NULL
           |       END AS owner_id
           |
           |FROM genea_multimedia
           |LEFT JOIN rel_events_multimedia ON rel_events_multimedia.media_id = genea_multimedia.media_id
           |LEFT JOIN rel_indi_multimedia ON rel_indi_multimedia.media_id = genea_multimedia.media_id
           |LEFT JOIN rel_familles_multimedia ON rel_familles_multimedia.media_id = genea_multimedia.media_id
           |LEFT JOIN rel_sour_citations_multimedia ON rel_sour_citations_multimedia.media_id = genea_multimedia.media_id
           |$where""".stripMargin
      )
        .on("id" -> id)
        .as[List[Media]](Media.mysqlParser.*)
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

  def getSurnamesList(id: Int)(implicit authenticatedRequest: AuthenticatedRequest[?]): Future[List[(String, Int)]] =
    Future {
      val mysqlParser: RowParser[(String, Int)] =
        (get[String]("indi_nom") ~
          get[Int]("count")).map {
          case name ~ count =>
            (name, count)
        }

      db.withConnection { implicit conn =>
        val excludePrivate = "AND indi_resn IS NULL"
        val isExcluded = authenticatedRequest.localSession.sessionData.userData.fold(excludePrivate) { userData =>
          if (userData.seePrivacy) "" else excludePrivate
        }
        SQL(s"""SELECT indi_nom, count(*) as count
               |FROM genea_individuals
               |WHERE base = {id} $isExcluded
               |GROUP BY indi_nom
               |ORDER BY indi_nom""".stripMargin)
          .on("id" -> id)
          .as(mysqlParser.*)
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
