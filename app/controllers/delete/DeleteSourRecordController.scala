package controllers.delete

import javax.inject.*

import scala.concurrent.ExecutionContext

import actions.AuthJourney
import cats.data.OptionT
import cats.implicits.*
import models.AuthenticatedRequest
import play.api.i18n.*
import play.api.mvc.*
import queries.DeleteSqlQueries
import queries.GetSqlQueries
import services.GenealogyDatabaseService
import views.html.delete.DeleteSourRecordView

@Singleton
class DeleteSourRecordController @Inject() (
    authJourney: AuthJourney,
    deleteSourRecordView: DeleteSourRecordView,
    getSqlQueries: GetSqlQueries,
    deleteSqlQueries: DeleteSqlQueries,
    genealogyDatabaseService: GenealogyDatabaseService,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def deleteSourRecordConfirmation(baseId: Int, id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      (for {
        database   <- OptionT(genealogyDatabaseService.getGenealogyDatabase(baseId))
        sourRecord <- getSqlQueries.getSourRecord(id)
      } yield {
        Ok(deleteSourRecordView(Some(database), sourRecord))
      }).getOrElse(NotFound("Database or source record not found"))
  }

  def deleteSourRecordAction(baseId: Int, id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      deleteSqlQueries.deleteSourRecord(id).map { _ =>
        Redirect(controllers.routes.SourRecordsController.index(baseId, None))
      }
  }

}
