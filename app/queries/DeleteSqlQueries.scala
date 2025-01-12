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
      SQL("""DELETE genea_events_details FROM genea_events_details
            | LEFT JOIN rel_indi_events ON genea_events_details.events_details_id = rel_indi_events.events_details_id
            | WHERE rel_indi_events.indi_id = {id}
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
  }(databaseExecutionContext)

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
  }(databaseExecutionContext)

}
