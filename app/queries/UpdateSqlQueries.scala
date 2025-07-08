package queries

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.Future

import anorm.*
import models.*
import models.queryData.FamilyQueryData
import models.EventType.FamilyEvent
import models.EventType.IndividualEvent
import models.EventType.UnknownEvent
import play.api.db.Database

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
          "resn"      -> personDetails.privacyRestriction
            .map(_.toString),
          "id" -> personDetails.id
        )
        .executeUpdate()
    }
  }(using databaseExecutionContext)

  def updateEventDetails(event: EventDetail): Future[Int] = Future {
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
        case _: FamilyEvent.type =>
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
        case _: IndividualEvent.type =>
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

        case _: UnknownEvent.type => 0
      }

    }
  }(using databaseExecutionContext)

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
            |sour_records_id = {sour_records_id},
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
          "sour_records_id"           -> sourCitation.record.map(_.id),
          "sour_citations_timestamp"  -> Instant.now,
          "id"                        -> sourCitation.id
        )
        .executeUpdate()
    }
  }(using databaseExecutionContext)

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
  }(using databaseExecutionContext)

  def updateEventNumberOfDays(eventId: Int, days: Option[Long]): Future[Int] = Future {
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
  }(using databaseExecutionContext)

  def deletePartnerFromFamily(partnerId: Int, familyId: Int): Future[Int] = Future {
    db.withTransaction { implicit conn =>
      val maybeFamily = SQL("""SELECT *
                              |FROM genea_familles
                              |WHERE familles_id = {id}""".stripMargin)
        .on("id" -> familyId)
        .as[Option[FamilyQueryData]](FamilyQueryData.mysqlParser.singleOpt)

      maybeFamily.fold(0) { family =>
        val setPartner = (family.parent1, family.parent2) match {
          case (Some(parent1), _) if parent1 == partnerId => "familles_husb = NULL"
          case (_, Some(parent2)) if parent2 == partnerId => "familles_wife = NULL"
          case (a, b)                                     => throw new RuntimeException(s"Combination $a, $b invalid to update partner in family")
        }

        SQL(s"""UPDATE genea_familles
               |SET $setPartner
               |WHERE familles_id = {id}
               |""".stripMargin)
          .on(
            "id" -> familyId
          )
          .executeUpdate()
      }
    }
  }(using databaseExecutionContext)

  def updatePartnerFromFamily(partnerId: Int, familyId: Int): Future[Int] = Future {
    db.withTransaction { implicit conn =>
      val maybeFamily = SQL("""SELECT *
                              |FROM genea_familles
                              |WHERE familles_id = {id}""".stripMargin)
        .on("id" -> familyId)
        .as[Option[FamilyQueryData]](FamilyQueryData.mysqlParser.singleOpt)

      maybeFamily.fold(0) { family =>
        val setPartner = (family.parent1, family.parent2) match {
          case (None, Some(id)) if id != partnerId => "familles_husb = {partnerId}"
          case (Some(id), None) if id != partnerId => "familles_wife = {partnerId}"
          case (None, None)                        => "familles_husb = {partnerId}"
          case (a, b)                              => throw new RuntimeException(s"Combination $a, $b invalid to update partner in family")
        }

        SQL(s"""UPDATE genea_familles
               |SET $setPartner
               |WHERE familles_id = {id}
               |""".stripMargin)
          .on(
            "partnerId" -> partnerId,
            "id"        -> familyId
          )
          .executeUpdate()
      }
    }

  }(using databaseExecutionContext)

}
