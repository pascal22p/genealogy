package queries

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.Future

import anorm.*
import models.*
import play.api.db.Database

@Singleton
final class DeleteSqlQueries @Inject() (db: Database, databaseExecutionContext: DatabaseExecutionContext) {

  def deletePersonDetails(personDetailsId: Int): Future[Int] = Future {
    db.withTransaction { implicit conn =>
      SQL("""DELETE FROM rel_familles_indi
            | WHERE rel_familles_indi.indi_id = {id}
        """.stripMargin)
        .on(
          "id" -> personDetailsId
        )
        .executeUpdate()

      SQL("""DELETE FROM rel_indi_events
            | WHERE rel_indi_events.indi_id = {id}
        """.stripMargin)
        .on(
          "id" -> personDetailsId
        )
        .executeUpdate()

      SQL("""DELETE FROM rel_indi_attributes
            | WHERE rel_indi_attributes.indi_id = {id}
        """.stripMargin)
        .on(
          "id" -> personDetailsId
        )
        .executeUpdate()

      SQL("""DELETE FROM rel_indi_multimedia
            | WHERE rel_indi_multimedia.indi_id = {id}
        """.stripMargin)
        .on(
          "id" -> personDetailsId
        )
        .executeUpdate()

      SQL("""DELETE FROM rel_indi_notes
            | WHERE rel_indi_notes.indi_id = {id}
        """.stripMargin)
        .on(
          "id" -> personDetailsId
        )
        .executeUpdate()

      SQL("""DELETE FROM rel_indi_refn
            | WHERE rel_indi_refn.indi_id = {id}
        """.stripMargin)
        .on(
          "id" -> personDetailsId
        )
        .executeUpdate()

      SQL("""DELETE FROM rel_indi_sources
            | WHERE rel_indi_sources.indi_id = {id}
        """.stripMargin)
        .on(
          "id" -> personDetailsId
        )
        .executeUpdate()

      SQL("""DELETE FROM genea_individuals
            | WHERE indi_id = {id}
          """.stripMargin)
        .on(
          "id" -> personDetailsId
        )
        .executeUpdate()
    }
  }(using databaseExecutionContext)

  def deleteGenealogyDatabase(id: Int): Future[Int] = Future {
    db.withConnection { implicit conn =>
      SQL("""DELETE FROM genea_infos
            | WHERE id = {id}
        """.stripMargin)
        .on(
          "id" -> id
        )
        .executeUpdate()
    }
  }(using databaseExecutionContext)

  def deleteChildFromFamily(childId: Int, familyId: Int): Future[Int] = Future {
    db.withConnection { implicit conn =>
      SQL("""DELETE FROM rel_familles_indi
            | WHERE indi_id = {childId} AND familles_id = {familyId}
          """.stripMargin)
        .on(
          "childId"  -> childId,
          "familyId" -> familyId
        )
        .executeUpdate()
    }
  }(using databaseExecutionContext)

  def deleteMedia(mediaId: Int): Future[Int] = Future {
    db.withConnection { implicit conn =>
      SQL("""DELETE FROM genea_multimedia
            | WHERE media_id = {mediaId}
        """.stripMargin)
        .on(
          "mediaId" -> mediaId
        )
        .executeUpdate()
    }
  }(using databaseExecutionContext)

  def deleteFamily(familyId: Int): Future[Int] = Future {
    db.withTransaction { implicit conn =>
      SQL("""DELETE FROM rel_familles_events
            | WHERE rel_familles_events.familles_id = {id}
        """.stripMargin)
        .on(
          "id" -> familyId
        )
        .executeUpdate()

      SQL("""DELETE FROM rel_familles_indi
            | WHERE rel_familles_indi.familles_id = {id}
        """.stripMargin)
        .on(
          "id" -> familyId
        )
        .executeUpdate()

      SQL("""DELETE FROM rel_familles_notes
            | WHERE rel_familles_notes.familles_id = {id}
        """.stripMargin)
        .on(
          "id" -> familyId
        )
        .executeUpdate()

      SQL("""DELETE FROM genea_familles
            | WHERE familles_id = {familyId}
        """.stripMargin)
        .on(
          "familyId" -> familyId
        )
        .executeUpdate()
    }
  }(using databaseExecutionContext)

  def deleteEvent(eventId: Int): Future[Int] = Future {
    db.withTransaction { implicit conn =>
      SQL("""DELETE FROM rel_events_sources
            | WHERE rel_events_sources.events_details_id = {id}
        """.stripMargin)
        .on(
          "id" -> eventId
        )
        .executeUpdate()

      SQL("""DELETE FROM rel_events_notes
            | WHERE rel_events_notes.events_details_id = {id}
        """.stripMargin)
        .on(
          "id" -> eventId
        )
        .executeUpdate()

      SQL("""DELETE FROM genea_events_details
            | WHERE events_details_id = {eventId}
        """.stripMargin)
        .on(
          "eventId" -> eventId
        )
        .executeUpdate()
    }
  }(using databaseExecutionContext)

  def deleteSourCitation(sourCitationId: Int): Future[Int] = Future {
    db.withTransaction { implicit conn =>
      SQL("""DELETE FROM rel_sour_citations_multimedia
            | WHERE rel_sour_citations_multimedia.sour_citations_id = {id}
        """.stripMargin)
        .on(
          "id" -> sourCitationId
        )
        .executeUpdate()

      SQL("""DELETE FROM genea_sour_citations
            | WHERE sour_citations_id = {sourCitationId}
        """.stripMargin)
        .on(
          "sourCitationId" -> sourCitationId
        )
        .executeUpdate()
    }
  }(using databaseExecutionContext)

}
