package queries

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import anorm.*
import models.*
import play.api.db.Database

@Singleton
final class GetCitationUsagesSqlQueries @Inject() (
    db: Database,
    databaseExecutionContext: DatabaseExecutionContext
)(implicit ec: ExecutionContext) {

  /**
   * Get all GEDCOM object usages for one known source citation.
   *
   * sourCitationId is the sour_citations_id.
   *
   * Queries are deliberately executed serially:
   *   1. Individuals
   *   2. Families
   *   3. Events
   */
  def getSourCitationUsages(
      sourCitationId: Int
  ): Future[List[GedcomObjectUsage]] = {

    for {
      individualUsages <- getCitationIndividualUsages(sourCitationId)
      familyUsages     <- getCitationFamilyUsages(sourCitationId)
      eventUsages      <- getCitationEventUsages(sourCitationId)
    } yield {
      individualUsages ++
        familyUsages ++
        eventUsages
    }
  }

  private def getCitationIndividualUsages(
      sourCitationId: Int
  ): Future[List[GedcomObjectUsage]] = Future {
    db.withConnection { implicit conn =>
      SQL(
        """
          |SELECT
          |    sour_citations_id,
          |    indi_id
          |FROM rel_indi_sources
          |WHERE sour_citations_id = {sourCitationId}
          |""".stripMargin
      )
        .on("sourCitationId" -> sourCitationId)
        .as(
          (SqlParser.get[Int]("sour_citations_id") ~ SqlParser.get[Int]("indi_id")).map {
            case citationId ~ indiId =>
              GedcomObjectUsage(
                gedcomObjectId = citationId,
                ownerType = GedcomObjectType.Individual,
                ownerId = indiId,
                personIds = List(indiId)
              )
          }.*
        )
    }
  }(using databaseExecutionContext)

  private def getCitationFamilyUsages(
      sourCitationId: Int
  ): Future[List[GedcomObjectUsage]] = Future {
    db.withConnection { implicit conn =>
      SQL(
        """
          |SELECT
          |    rfs.sour_citations_id,
          |    rfs.familles_id,
          |    f.familles_husb,
          |    f.familles_wife
          |FROM rel_familles_sources rfs
          |JOIN genea_familles f
          |    ON f.familles_id = rfs.familles_id
          |WHERE rfs.sour_citations_id = {sourCitationId}
          |""".stripMargin
      )
        .on("sourCitationId" -> sourCitationId)
        .as(
          (SqlParser.get[Int]("sour_citations_id") ~
            SqlParser.get[Int]("familles_id") ~
            SqlParser.get[Option[Int]]("familles_husb") ~
            SqlParser.get[Option[Int]]("familles_wife")).map {
            case citationId ~ familleId ~ husbandId ~ wifeId =>
              GedcomObjectUsage(
                gedcomObjectId = citationId,
                ownerType = GedcomObjectType.Family,
                ownerId = familleId,
                personIds = List(husbandId, wifeId).flatten.distinct
              )
          }.*
        )
    }
  }(using databaseExecutionContext)

  private def getCitationEventUsages(
      sourCitationId: Int
  ): Future[List[GedcomObjectUsage]] = Future {
    db.withConnection { implicit conn =>
      /*
       * Start from rel_events_sources.
       *
       * This is important: an event citation must still produce an
       * Event usage even when the event has no individual/family
       * relationship. Person resolution is optional enrichment.
       */
      val eventUsages =
        SQL(
          """
            |SELECT DISTINCT
            |    sour_citations_id,
            |    events_details_id
            |FROM rel_events_sources
            |WHERE sour_citations_id = {sourCitationId}
            |""".stripMargin
        )
          .on("sourCitationId" -> sourCitationId)
          .as(
            (SqlParser.get[Int]("sour_citations_id") ~ SqlParser.get[Int]("events_details_id")).map {
              case citationId ~ eventId =>
                GedcomObjectUsage(
                  gedcomObjectId = citationId,
                  ownerType = GedcomObjectType.Event,
                  ownerId = eventId
                )
            }.*
          )

      /*
       * Individual event relationships.
       */
      val individualEvents =
        SQL(
          """
            |SELECT
            |    res.events_details_id,
            |    rie.indi_id
            |FROM rel_events_sources res
            |JOIN rel_indi_events rie
            |    ON rie.events_details_id = res.events_details_id
            |WHERE res.sour_citations_id = {sourCitationId}
            |""".stripMargin
        )
          .on("sourCitationId" -> sourCitationId)
          .as(
            (SqlParser.get[Int]("events_details_id") ~ SqlParser.get[Int]("indi_id")).map {
              case eventId ~ indiId =>
                eventId -> List(indiId)
            }.*
          )

      /*
       * Individual attribute relationships.
       */
      val individualAttributes =
        SQL(
          """
            |SELECT
            |    res.events_details_id,
            |    ria.indi_id
            |FROM rel_events_sources res
            |JOIN rel_indi_attributes ria
            |    ON ria.events_details_id = res.events_details_id
            |WHERE res.sour_citations_id = {sourCitationId}
            |""".stripMargin
        )
          .on("sourCitationId" -> sourCitationId)
          .as(
            (SqlParser.get[Int]("events_details_id") ~ SqlParser.get[Int]("indi_id")).map {
              case eventId ~ indiId =>
                eventId -> List(indiId)
            }.*
          )

      /*
       * Family event relationships.
       *
       * A family event contributes the husband and wife, when present.
       */
      val familyEvents =
        SQL(
          """
            |SELECT
            |    res.events_details_id,
            |    f.familles_husb,
            |    f.familles_wife
            |FROM rel_events_sources res
            |JOIN rel_familles_events rfe
            |    ON rfe.events_details_id = res.events_details_id
            |JOIN genea_familles f
            |    ON f.familles_id = rfe.familles_id
            |WHERE res.sour_citations_id = {sourCitationId}
            |""".stripMargin
        )
          .on("sourCitationId" -> sourCitationId)
          .as(
            (SqlParser.get[Int]("events_details_id") ~
              SqlParser.get[Option[Int]]("familles_husb") ~
              SqlParser.get[Option[Int]]("familles_wife")).map {
              case eventId ~ husbandId ~ wifeId =>
                eventId -> List(husbandId, wifeId).flatten
            }.*
          )

      val peopleByEvent =
        (
          individualEvents ++
            individualAttributes ++
            familyEvents
        )
          .groupMapReduce(_._1)(_._2)(_ ++ _)
          .view
          .mapValues(_.distinct)
          .toMap

      eventUsages.map { usage =>
        usage.copy(
          personIds = peopleByEvent.getOrElse(
            usage.ownerId,
            Nil
          )
        )
      }
    }
  }(using databaseExecutionContext)
}
