package queries

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.Future

import anorm.*
import anorm.SqlParser.*
import cats.data.OptionT
import models.*
import models.forms.EventDetailForm
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
          "resn"      -> personDetails.privacyRestriction
        )
        .executeInsert[Option[Int]](parser)
    }
  }(databaseExecutionContext))

  def insertEventDetail(
      eventDetailOnlyQueryData: EventDetailOnlyQueryData,
      ownerId: Int,
      eventType: EventType
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
              "place_id"                   -> eventDetailOnlyQueryData.place_id,
              "addr_id"                    -> eventDetailOnlyQueryData.addr_id,
              "events_details_descriptor"  -> eventDetailOnlyQueryData.events_details_descriptor,
              "events_details_gedcom_date" -> eventDetailOnlyQueryData.events_details_gedcom_date,
              "events_details_age"         -> eventDetailOnlyQueryData.events_details_age,
              "events_details_cause"       -> eventDetailOnlyQueryData.events_details_cause,
              "jd_count"                   -> eventDetailOnlyQueryData.jd_count,
              "jd_precision"               -> eventDetailOnlyQueryData.jd_precision,
              "jd_calendar"                -> eventDetailOnlyQueryData.jd_calendar,
              "events_details_famc"        -> eventDetailOnlyQueryData.events_details_famc,
              "events_details_adop"        -> eventDetailOnlyQueryData.events_details_adop,
              "base"                       -> eventDetailOnlyQueryData.base
            )
            .executeInsert[Option[Int]](parser)

        if (eventType == IndividualEvent) {
          SQL("""INSERT INTO rel_indi_events
                | (indi_id, events_details_id)
                | VALUES ({indiId}, {eventId})
        """.stripMargin)
            .on(
              "indiId"  -> ownerId,
              "eventId" -> eventId
            )
            .execute()
        } else if (eventType == FamilyEvent) {
          SQL("""INSERT INTO rel_familles_events
                | (familles_id, events_details_id)
                | VALUES ({familyId}, {eventId})
        """.stripMargin)
            .on(
              "familyId" -> ownerId,
              "eventId"  -> eventId
            )
            .execute()
        } else {
          val ex = new RuntimeException(s"The type `$eventType` is not supported")
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

}
