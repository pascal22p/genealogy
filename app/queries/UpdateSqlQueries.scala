package queries

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.Future

import anorm.*
import anorm.SqlParser.*
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

@Singleton
final class UpdateSqlQueries @Inject() (db: Database, databaseExecutionContext: DatabaseExecutionContext) {

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

  def updateEventDetails(event: EventDetail) = Future {
    db.withTransaction { implicit conn =>
      SQL("""UPDATE genea_events_details
            |SET place_id = {place_id},
            |addr_id = {addr_id},
            |events_details_descriptor = {events_details_descriptor},
            |events_details_gedcom_date = {events_details_gedcom_date},
            |events_details_age = {events_details_age},
            |events_details_cause = {events_details_cause},
            |base = {base},
            |events_details_timestamp = {timestamp}
            |WHERE events_details_id = {id}
            |""".stripMargin)
        .on(
          "id"                         -> event.events_details_id,
          "place_id"                   -> event.place.map(_.id),
          "addr_id"                    -> event.addr_id,
          "events_details_descriptor"  -> event.events_details_descriptor,
          "events_details_gedcom_date" -> event.events_details_gedcom_date,
          "events_details_age"         -> event.events_details_age,
          "events_details_cause"       -> event.events_details_cause,
          "base"                       -> event.base,
          "timestamp"                  -> Instant.now
        )
        .executeUpdate()

      event.eventType match {
        case FamilyEvent =>
          SQL("""UPDATE rel_familles_events
                |SET events_tag = {tag},
                |timestamp = {timestamp}
                |WHERE events_details_id = {id}
                |""".stripMargin)
            .on(
              "id"        -> event.events_details_id,
              "tag"       -> event.tag,
              "timestamp" -> Instant.now
            )
            .executeUpdate()
        case IndividualEvent =>
          SQL("""UPDATE rel_indi_events
                |SET events_tag = {tag},
                |timestamp = {timestamp}
                |WHERE events_details_id = {id}
                |""".stripMargin)
            .on(
              "id"        -> event.events_details_id,
              "tag"       -> event.tag,
              "timestamp" -> Instant.now
            )
            .executeUpdate()

        case UnknownEvent => 0
      }

    }
  }(databaseExecutionContext)

  def updateSourCitation(sourCitation: SourCitation): Future[Int] = Future {
    db.withConnection { implicit conn =>
      SQL("""UPDATE genea_sour_citations
            |SET sour_citations_page = {sour_citations_page},
            |sour_citations_even = {sour_citations_even},
            |sour_citations_even_role = {sour_citations_even_role},
            |sour_citations_data_dates = {sour_citations_data_dates},
            |sour_citations_data_text = {sour_citations_data_text},
            |sour_citations_quay = {sour_citations_quay},
            |sour_citations_subm = {sour_citations_subm},
            |sour_citations_timestamp = {sour_citations_timestamp}
            |WHERE sour_citations_id = {id}
            |""".stripMargin)
        .on(
          "sour_citations_page"       -> sourCitation.page,
          "sour_citations_even"       -> sourCitation.even,
          "sour_citations_even_role"  -> sourCitation.role,
          "sour_citations_data_dates" -> sourCitation.dates,
          "sour_citations_data_text"  -> sourCitation.text,
          "sour_citations_quay"       -> sourCitation.quay,
          "sour_citations_subm"       -> sourCitation.subm,
          "sour_citations_timestamp"  -> Instant.now,
          "id"                        -> sourCitation.id
        )
        .executeUpdate()
    }
  }(databaseExecutionContext)

  def updateSourRecord(sourRecord: SourRecord): Future[Int] = Future {
    db.withConnection { implicit conn =>
      SQL("""UPDATE genea_sour_records
            |SET sour_records_auth = {sour_records_auth},
            |sour_records_title = {sour_records_title},
            |sour_records_abbr = {sour_records_abbr},
            |sour_records_publ = {sour_records_publ},
            |sour_records_agnc = {sour_records_agnc},
            |sour_records_rin = {sour_records_rin},
            |repo_id = {repo_id},
            |repo_caln = {repo_caln},
            |repo_medi = {repo_medi},
            |sour_records_timestamp = {sour_records_timestamp}
            |WHERE sour_records_id = {id}
            |""".stripMargin)
        .on(
          "sour_records_auth"      -> sourRecord.auth,
          "sour_records_title"     -> sourRecord.title,
          "sour_records_abbr"      -> sourRecord.abbr,
          "sour_records_publ"      -> sourRecord.publ,
          "sour_records_agnc"      -> sourRecord.agnc,
          "sour_records_rin"       -> sourRecord.rin,
          "repo_id"                -> sourRecord.repoId,
          "repo_caln"              -> sourRecord.repoCaln,
          "repo_medi"              -> sourRecord.repoMedi,
          "sour_records_timestamp" -> Instant.now,
          "id"                     -> sourRecord.id
        )
        .executeUpdate()
    }
  }(databaseExecutionContext)

  def updateEventNumberofDays(eventId: Int, days: Option[Long]): Future[Int] = Future {
    db.withConnection { implicit conn =>
      SQL("""UPDATE genea_events_details
            |SET jd_count = {days}
            |WHERE events_details_id = {eventId}
            |""".stripMargin)
        .on(
          "days"    -> days,
          "eventId" -> eventId
        )
        .executeUpdate()
    }
  }(databaseExecutionContext)

}
