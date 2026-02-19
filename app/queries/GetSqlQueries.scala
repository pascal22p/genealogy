package queries

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.Future

import anorm.*
import anorm.SqlParser.*
import cats.data.OptionT
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

@Singleton
final class GetSqlQueries @Inject() (
    db: Database,
    databaseExecutionContext: DatabaseExecutionContext
) {

  def getPersonDetails(id: Int): Future[List[PersonDetails]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM genea_individuals
            |WHERE indi_id = {id}""".stripMargin)
        .on("id" -> id)
        .as[List[PersonDetails]](PersonDetails.mysqlParser.*)
    }
  }(using databaseExecutionContext)

  def getLatestPersonDetails(dbId: Int, maxNumber: Int): Future[List[PersonDetails]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM genea_individuals
            |WHERE base = {base}
            |ORDER BY indi_timestamp DESC
            |LIMIT {max}""".stripMargin)
        .on("base" -> dbId, "max" -> maxNumber)
        .as[List[PersonDetails]](PersonDetails.mysqlParser.*)
    }
  }(using databaseExecutionContext)
  def searchIndividuals(dbId: Int, words: Seq[String]): Future[List[PersonDetails]] = Future {
    db.withConnection { implicit conn =>
      val wordConditions =
        words.indices.map(i => s"(indi_nom LIKE {word$i} OR indi_prenom LIKE {word$i})").mkString(" AND ")
      val query =
        s"""SELECT *
           |FROM genea_individuals
           |WHERE base = {base}
           |AND ($wordConditions)
           |ORDER BY indi_nom, indi_prenom
           |LIMIT 50""".stripMargin

      val parameters = Seq[NamedParameter]("base" -> dbId) ++ words.zipWithIndex.map {
        case (word, i) => NamedParameter(s"word$i", s"%$word%")
      }

      println(query)
      println(parameters)

      SQL(query)
        .on(parameters*)
        .as[List[PersonDetails]](PersonDetails.mysqlParser.*)
    }
  }(using databaseExecutionContext)

  def getEvents(id: Int, eventType: EventType): Future[List[EventDetailQueryData]] = Future {
    db.withConnection { implicit conn =>
      val where = eventType match {
        case _: IndividualEvent.type => "WHERE rel_indi_events.indi_id = {id}"
        case _: FamilyEvent.type     => "WHERE rel_familles_events.familles_id = {id}"
        case _                       => "WHERE genea_events_details.events_details_id = {id}"
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
  }(using databaseExecutionContext)

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
  }(using databaseExecutionContext)

  def getFamilyIdsFromPartnerId(individualId: Int): Future[List[Int]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT familles_id
            |FROM genea_familles
            |WHERE familles_husb = {id} OR familles_wife = {id}""".stripMargin)
        .on("id" -> individualId)
        .as[List[Int]](int("familles_id").*)
    }
  }(using databaseExecutionContext)

  def getFamilyDetails(familyId: Int): OptionT[Future, FamilyQueryData] = OptionT(Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM genea_familles
            |WHERE familles_id = {id}""".stripMargin)
        .on("id" -> familyId)
        .as[Option[FamilyQueryData]](FamilyQueryData.mysqlParser.singleOpt)
    }
  }(using databaseExecutionContext))

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
  }(using databaseExecutionContext)

  def getPlace(id: Int): OptionT[Future, Place] = OptionT(Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM genea_place
            |WHERE place_id = {id}""".stripMargin)
        .on("id" -> id)
        .as[Option[Place]](Place.mysqlParser.singleOpt)
    }
  }(using databaseExecutionContext))

  def getAllPlaces: Future[List[Place]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM genea_place""".stripMargin)
        .as[List[Place]](Place.mysqlParser.*)
    }
  }(using databaseExecutionContext)

  def getSourCitations(id: Int, typeCitation: SourCitationType, dbId: Int): Future[List[SourCitationQueryData]] =
    Future {
      db.withConnection { implicit conn =>
        val where = (typeCitation match {
          case _: EventSourCitation.type      => "WHERE rel_events_sources.events_details_id = {id}"
          case _: IndividualSourCitation.type => "WHERE rel_indi_sources.indi_id = {id}"
          case _: FamilySourCitation.type     => "WHERE rel_familles_sources.familles_id = {id}"
          case _: UnknownSourCitation.type    => "WHERE genea_sour_citations.sour_citations_id = {id}"
        }) + " AND genea_sour_citations.base = {dbId}"

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
          .on(
            "id"   -> id,
            "dbId" -> dbId
          )
          .as[List[SourCitationQueryData]](SourCitationQueryData.mysqlParser.*)
      }
    }(using databaseExecutionContext)

  def getMedias(id: Option[Int] = None, typeMedia: MediaType, dbId: Int): Future[List[Media]] = Future {
    db.withConnection { implicit conn =>
      val where: String = if (id.isDefined) {
        typeMedia match {
          case _: EventMedia.type =>
            "WHERE rel_events_multimedia.events_details_id = {id} AND genea_multimedia.base = {dbId}"
          case _: IndividualMedia.type => "WHERE rel_indi_multimedia.indi_id = {id} AND genea_multimedia.base = {dbId}"
          case _: FamilyMedia.type     =>
            "WHERE rel_familles_multimedia.familles_id = {id} AND genea_multimedia.base = {dbId}"
          case _: SourCitationMedia.type =>
            "WHERE rel_sour_citations_multimedia.sour_citations_id = {id} AND genea_multimedia.base = {dbId}"
          case _: UnknownMedia.type => "WHERE genea_multimedia.media_id = {id} AND genea_multimedia.base = {dbId}"
        }
      } else {
        "WHERE genea_multimedia.base = {dbId}"
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
        .on(
          "id"   -> id,
          "dbId" -> dbId
        )
        .as[List[Media]](Media.mysqlParser.*)
    }
  }(using databaseExecutionContext)

  def getGenealogyDatabase(id: Int): OptionT[Future, GenealogyDatabase] = OptionT(Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT id, nom, descriptif, medias
            |FROM genea_infos
            |WHERE id = {id}""".stripMargin)
        .on("id" -> id)
        .as[Option[GenealogyDatabase]](GenealogyDatabase.mysqlParser.singleOpt)
    }
  }(using databaseExecutionContext))

  def getGenealogyDatabases: Future[List[GenealogyDatabase]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT id, nom, descriptif, medias
            |FROM genea_infos
            |ORDER BY nom""".stripMargin)
        .as[List[GenealogyDatabase]](GenealogyDatabase.mysqlParser.*)
    }
  }(using databaseExecutionContext)

  def getSurnamesList(
      id: Int
  )(implicit authenticatedRequest: AuthenticatedRequest[?]): Future[List[(String, Int, Option[Int], Option[Int])]] =
    Future {
      val mysqlParser: RowParser[(String, Int, Option[Int], Option[Int])] =
        (get[String]("indi_nom") ~
          get[Int]("unique_count") ~
          get[Option[Int]]("min_jd_count") ~
          get[Option[Int]]("max_jd_count")).map {
          case name ~ count ~ minDayCount ~ maxDayCount =>
            (name, count, minDayCount, maxDayCount)
        }

      db.withConnection { implicit conn =>
        val excludePrivate = "AND indi_resn IS NULL"
        val isExcluded     = authenticatedRequest.localSession.sessionData.userData.fold(excludePrivate) { userData =>
          if (userData.seePrivacy) "" else excludePrivate
        }
        SQL(
          s"""SELECT
             |    i.indi_nom,
             |    COUNT(*) AS unique_count,
             |    MIN(e.min_jd) AS min_jd_count,
             |    MAX(e.max_jd) AS max_jd_count
             |FROM genea_individuals i
             |LEFT JOIN (
             |    SELECT
             |        rie.indi_id,
             |        MIN(d.jd_count) AS min_jd,
             |        MAX(d.jd_count) AS max_jd
             |    FROM rel_indi_events rie
             |    JOIN genea_events_details d
             |      ON d.events_details_id = rie.events_details_id
             |    GROUP BY rie.indi_id
             |) e ON e.indi_id = i.indi_id
             |WHERE i.base = {id}
             |  $isExcluded
             |GROUP BY i.indi_nom
             |ORDER BY i.indi_nom;
             |""".stripMargin
        )
          .on("id" -> id)
          .as(mysqlParser.*)
      }
    }(using databaseExecutionContext)

  def getFirstNamesList(
      dbId: Int,
      name: String,
      pageSize: Int,
      cursor: Option[(String, Int, Int, Int)] = None,
      reverse: Boolean = false
  )(
      implicit authenticatedRequest: AuthenticatedRequest[?]
  ): Future[Seq[FirstnameWithBirthDeath]] = Future {
    val compareTo = if (reverse) { "<" }
    else { ">" }
    val order = if (reverse) { "DESC" }
    else { "ASC" }

    val excludePrivate = "AND indi_resn IS NULL"
    val isExcluded     = authenticatedRequest.localSession.sessionData.userData.fold(excludePrivate) { userData =>
      if (userData.seePrivacy) "" else excludePrivate
    }
    val cursorPagination =
      cursor.fold("")(_ =>
        s"""
           |  AND (
           |     t.indi_prenom $compareTo {prenom}
           |  OR (t.indi_prenom = {prenom} AND COALESCE(birth_date_jd, 0) $compareTo {birthJd})
           |  OR (t.indi_prenom = {prenom} AND COALESCE(birth_date_jd, 0) = {birthJd} AND COALESCE(death_date_jd, 0) $compareTo {deathJd})
           |  OR (t.indi_prenom = {prenom} AND COALESCE(birth_date_jd, 0) = {birthJd}
           |      AND COALESCE(death_date_jd, 0) = {deathJd} AND t.indi_id $compareTo {id})
           |  )""".stripMargin
      )

    val parameters = Seq[NamedParameter](
      "id"       -> dbId,
      "name"     -> name,
      "pageSize" -> pageSize
    ) ++
      cursor.fold(Seq.empty) {
        case (prenom, id, birthJd, deathJd) =>
          Seq[NamedParameter](
            "prenom"  -> prenom,
            "lastId"  -> id,
            "birthJd" -> birthJd,
            "deathJd" -> deathJd
          )
      }

    db.withConnection { implicit conn =>
      SQL(
        s"""SELECT *
           |FROM (
           |    SELECT
           |        i.indi_id,
           |        i.indi_prenom,
           |
           |        MIN(CASE
           |              WHEN rie.events_tag IN ('BIRT', 'CHR', 'BAPM')
           |              THEN d.events_details_gedcom_date
           |            END) AS birth_date,
           |
           |        MIN(CASE
           |              WHEN rie.events_tag IN ('BIRT', 'CHR', 'BAPM')
           |              THEN d.jd_count
           |            END) AS birth_date_jd,
           |
           |        MIN(CASE
           |              WHEN rie.events_tag IN ('DEAT', 'BURI', 'CREM')
           |              THEN d.events_details_gedcom_date
           |            END) AS death_date,
           |
           |        MIN(CASE
           |              WHEN rie.events_tag IN ('DEAT', 'BURI', 'CREM')
           |              THEN d.jd_count
           |            END) AS death_date_jd
           |
           |    FROM genea_individuals i
           |    LEFT JOIN rel_indi_events rie ON rie.indi_id = i.indi_id
           |    LEFT JOIN genea_events_details d ON d.events_details_id = rie.events_details_id
           |    WHERE i.base = {id}
           |      AND i.indi_nom = {name}
           |      $isExcluded
           |    GROUP BY i.indi_id, i.indi_prenom
           |) t
           |WHERE 1 = 1
           |  $cursorPagination
           |ORDER BY
           |  t.indi_prenom $order,
           |  COALESCE(t.birth_date_jd, 0) $order,
           |  COALESCE(t.death_date_jd, 0) $order,
           |  t.indi_id $order
           |LIMIT {pageSize}
           |""".stripMargin
      )
        .on(parameters*)
        .as(FirstnameWithBirthDeath.mysqlParser.*)
    }
  }(using databaseExecutionContext)

  def getAllPersonDetails(dbId: Int, name: Option[String] = None)(
      implicit authenticatedRequest: AuthenticatedRequest[?]
  ): Future[List[PersonDetails]] = Future {
    db.withConnection { implicit conn =>
      val excludePrivate = "AND indi_resn IS NULL"
      val isExcluded     = authenticatedRequest.localSession.sessionData.userData.fold(excludePrivate) { userData =>
        if (userData.seePrivacy) "" else excludePrivate
      }
      val filter = name.fold("") { _ =>
        "AND indi_nom = {name}"
      }

      SQL(s"""SELECT *
             |FROM genea_individuals
             |WHERE base = {id} $filter $isExcluded
             |ORDER BY indi_prenom""".stripMargin)
        .on("id" -> dbId, "name" -> name.getOrElse(""))
        .as(PersonDetails.mysqlParser.*)
    }
  }(using databaseExecutionContext)

  def getUserData(username: String): OptionT[Future, UserData] = OptionT(Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM genea_membres
            |WHERE email = {email}""".stripMargin)
        .on("email" -> username)
        .as(UserData.mysqlParser.singleOpt)
    }
  }(using databaseExecutionContext))

  def getSourRecord(id: Int): OptionT[Future, SourRecord] = OptionT(Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM genea_sour_records
            |WHERE sour_records_id = {id}""".stripMargin)
        .on("id" -> id)
        .as[Option[SourRecord]](SourRecord.mysqlParser.singleOpt)
    }
  }(using databaseExecutionContext))

  def getAllEvents: Future[List[EventDetailOnlyQueryData]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM genea_events_details""".stripMargin)
        .as[List[EventDetailOnlyQueryData]](EventDetailOnlyQueryData.mysqlParser.*)
    }
  }(using databaseExecutionContext)

  def getAllSourRecords: Future[List[SourRecord]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM genea_sour_records""".stripMargin)
        .as[List[SourRecord]](SourRecord.mysqlParser.*)
    }
  }(using databaseExecutionContext)

  def getSourRecords(dbId: Int): Future[List[SourRecord]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM genea_sour_records
            |WHERE base = {dbId}""".stripMargin)
        .on("dbId" -> dbId)
        .as[List[SourRecord]](SourRecord.mysqlParser.*)
    }
  }(using databaseExecutionContext)

  def getAllFamilies(baseId: Int): Future[List[FamilyQueryData]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM genea_familles
            |WHERE base = {id}""".stripMargin)
        .on("id" -> baseId)
        .as[List[FamilyQueryData]](FamilyQueryData.mysqlParser.*)
    }
  }(using databaseExecutionContext)

  def getOrphanedIndividuals(baseId: Int): Future[List[PersonDetails]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT * FROM genea_individuals WHERE
            |base = {dbId} AND
            |NOT EXISTS (SELECT * FROM genea_familles WHERE indi_id = familles_wife OR indi_id = familles_husb) AND
            |NOT EXISTS (SELECT * FROM rel_familles_indi WHERE genea_individuals.indi_id = rel_familles_indi.indi_id)
            |""".stripMargin)
        .on("dbId" -> baseId)
        .as[List[PersonDetails]](PersonDetails.mysqlParser.*)
    }
  }(using databaseExecutionContext)

  def getOrphanedFamilies(baseId: Int): Future[List[FamilyQueryData]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT * FROM genea_familles WHERE
            |base = {dbId} AND
            |familles_wife IS NULL AND familles_husb IS NULL
            |""".stripMargin)
        .on("dbId" -> baseId)
        .as[List[FamilyQueryData]](FamilyQueryData.mysqlParser.*)
    }
  }(using databaseExecutionContext)

  def getOrphanedSourCitations(baseId: Int): Future[List[SourCitationQueryData]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT * FROM genea_sour_citations AS sources WHERE
            |base = {dbId} AND
            |NOT EXISTS (SELECT * FROM rel_events_sources WHERE sources.sour_citations_id = rel_events_sources.sour_citations_id) AND
            |NOT EXISTS (SELECT * FROM rel_familles_sources WHERE sources.sour_citations_id = rel_familles_sources.sour_citations_id) AND
            |NOT EXISTS (SELECT * FROM rel_indi_sources WHERE sources.sour_citations_id = rel_indi_sources.sour_citations_id)
            |""".stripMargin)
        .on("dbId" -> baseId)
        .as[List[SourCitationQueryData]](SourCitationQueryData.mysqlParserCitationOnly.*)
    }
  }(using databaseExecutionContext)

  def getSourCitationsFromRecord(recordId: Int): Future[List[SourCitationQueryData]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT * FROM genea_sour_citations AS sources WHERE
            |sour_records_id = {recordId}""".stripMargin)
        .on("recordId" -> recordId)
        .as[List[SourCitationQueryData]](SourCitationQueryData.mysqlParserCitationOnly.*)
    }
  }(using databaseExecutionContext)

  def getMedia(baseId: Int, id: Int): OptionT[Future, Media] = OptionT(Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM genea_multimedia
            |WHERE base = {baseId} AND
            |media_id = {id}
            |""".stripMargin)
        .on("baseId" -> baseId, "id" -> id)
        .as[Option[Media]](Media.mysqlParserMediaOnly.singleOpt)
    }
  }(using databaseExecutionContext))

  def getOrphanedEvents(baseId: Int): Future[List[EventDetailQueryData]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM genea_events_details
            |WHERE base = {baseId} AND
            |NOT EXISTS (SELECT * FROM rel_indi_events WHERE genea_events_details.events_details_id = rel_indi_events.events_details_id) AND
            |NOT EXISTS (SELECT * FROM rel_indi_attributes WHERE genea_events_details.events_details_id = rel_indi_attributes.events_details_id) AND
            |NOT EXISTS (SELECT * FROM rel_familles_events WHERE genea_events_details.events_details_id = rel_familles_events.events_details_id)
            |""".stripMargin)
        .on("baseId" -> baseId)
        .as[List[EventDetailQueryData]](EventDetailQueryData.mysqlParserEventDetailOnly.*)
    }
  }(using databaseExecutionContext)

  def getAddresses(baseId: Int): Future[List[AddressQueryData]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM genea_address
            |WHERE base = {baseId}
            |""".stripMargin)
        .on("baseId" -> baseId)
        .as[List[AddressQueryData]](AddressQueryData.mysqlParserAddress.*)
    }
  }(using databaseExecutionContext)

  def getRepositories(baseId: Int): Future[List[RepositoryQueryData]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM genea_repository
            |LEFT JOIN genea_address ON genea_address.addr_id = genea_repository.addr_id
            |WHERE genea_repository.base = {baseId}
            |""".stripMargin)
        .on("baseId" -> baseId)
        .as[List[RepositoryQueryData]](RepositoryQueryData.mysqlParserRepository.*)
    }
  }(using databaseExecutionContext)

}
