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
