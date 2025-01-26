package queries

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.Future

import anorm.*
import anorm.SqlParser.*
import cats.data.OptionT
import models.*
import models.queryData.*
import models.EventType.FamilyEvent
import models.EventType.IndividualEvent
import play.api.db.Database
import play.api.Logging

@Singleton
final class InsertSqlQueries @Inject() (db: Database, databaseExecutionContext: DatabaseExecutionContext)
    extends Logging {

  def insertPersonDetails(personDetails: PersonDetails): OptionT[Future, Int] = OptionT(Future {
    val parser: ResultSetParser[Option[Int]] = {
      int("insert_id").singleOpt
    }

    db.withConnection { implicit conn =>
      SQL("""INSERT INTO genea_individuals 
            | (base, indi_nom, indi_prenom, indi_sexe, indi_npfx, indi_givn, indi_nick, indi_spfx, indi_nsfx, indi_resn)
            | VALUES ({base}, {surname}, {firstname},{sex}, {npfx}, {nameGiven}, {nickname}, {spfx}, {nsfx}, {resn})
          """.stripMargin)
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
          "resn"      -> personDetails.privacyRestriction.map(resn => s"$resn")
        )
        .executeInsert[Option[Int]](parser)
    }
  }(databaseExecutionContext))

  def insertEventDetail(
      eventDetailQueryData: EventDetailQueryData,
  ): OptionT[Future, Int] =
    OptionT(Future {
      val parser: ResultSetParser[Option[Int]] = {
        int("insert_id").singleOpt
      }

      db.withTransaction { implicit conn =>
        val eventId =
          SQL("""INSERT INTO genea_events_details 
                | (place_id, addr_id, events_details_descriptor, events_details_gedcom_date, events_details_age, events_details_cause, jd_count, jd_precision, jd_calendar, events_details_famc, events_details_adop, base)
                | VALUES ({place_id}, {addr_id}, {events_details_descriptor}, {events_details_gedcom_date}, {events_details_age}, {events_details_cause}, {jd_count}, {jd_precision}, {jd_calendar}, {events_details_famc}, {events_details_adop}, {base})
        """.stripMargin)
            .on(
              "place_id"                   -> eventDetailQueryData.place_id,
              "addr_id"                    -> eventDetailQueryData.addr_id,
              "events_details_descriptor"  -> eventDetailQueryData.events_details_descriptor,
              "events_details_gedcom_date" -> eventDetailQueryData.events_details_gedcom_date,
              "events_details_age"         -> eventDetailQueryData.events_details_age,
              "events_details_cause"       -> eventDetailQueryData.events_details_cause,
              "jd_count"                   -> eventDetailQueryData.jd_count,
              "jd_precision"               -> eventDetailQueryData.jd_precision,
              "jd_calendar"                -> eventDetailQueryData.jd_calendar,
              "events_details_famc"        -> eventDetailQueryData.events_details_famc,
              "events_details_adop"        -> eventDetailQueryData.events_details_adop,
              "base"                       -> eventDetailQueryData.base
            )
            .executeInsert[Option[Int]](parser)

        eventDetailQueryData.eventType match {
          case _: IndividualEvent.type =>
            SQL("""INSERT INTO rel_indi_events
                  | (indi_id, events_details_id, events_tag)
                  | VALUES ({indiId}, {eventId}, {eventTag})
            """.stripMargin)
              .on(
                "indiId"   -> eventDetailQueryData.ownerId.getOrElse(0),
                "eventId"  -> eventId,
                "eventTag" -> eventDetailQueryData.tag
              )
              .execute()

          case _: FamilyEvent.type =>
            SQL("""INSERT INTO rel_familles_events
                  | (familles_id, events_details_id, events_tag)
                  | VALUES ({familyId}, {eventId}, {eventTag})
            """.stripMargin)
              .on(
                "familyId" -> eventDetailQueryData.ownerId,
                "eventId"  -> eventId,
                "eventTag" -> eventDetailQueryData.tag
              )
              .execute()

          case _ =>
            val ex = new RuntimeException(s"The type `${eventDetailQueryData.eventType}` is not supported")
            logger.error(ex.getMessage(), ex)
            None
        }
        eventId
      }
    }(databaseExecutionContext))

  def insertDatabase(genealogyDatabase: GenealogyDatabase): OptionT[Future, Int] = OptionT(Future {
    val parser: ResultSetParser[Option[Int]] = {
      int("insert_id").singleOpt
    }

    db.withConnection { implicit conn =>
      SQL(
        """INSERT INTO genea_infos 
          | (nom, descriptif, entetes)
          | VALUES ({name}, {description}, "{headers}")
        """.stripMargin
      )
        .on(
          "name"        -> genealogyDatabase.name,
          "description" -> genealogyDatabase.description,
          "headers"     -> ""
        )
        .executeInsert[Option[Int]](parser)
    }
  }(databaseExecutionContext))

  def insertSourCitation(sourCitation: SourCitationQueryData): OptionT[Future, Int] = OptionT(Future {
    val parser: ResultSetParser[Option[Int]] = {
      int("insert_id").singleOpt
    }

    db.withConnection { implicit conn =>
      SQL(
        """INSERT INTO genea_sour_citations 
          | (nom, descriptif, entetes)
          | VALUES ({name}, {description}, "{headers}")
        """.stripMargin
      )
        .on(
          "name"        -> sourCitation.id,
          "description" -> sourCitation.dates,
          "headers"     -> ""
        )
        .executeInsert[Option[Int]](parser)
    }
  }(databaseExecutionContext))

}
