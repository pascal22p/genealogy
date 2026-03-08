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
import services.GenealogyDatabaseService
import queries.GetSqlQueries
import views.html.delete.DeleteMediaView

@Singleton
class DeleteMediaController @Inject() (
    authJourney: AuthJourney,
    deleteMediaView: DeleteMediaView,
    getSqlQueries: GetSqlQueries,
    deleteSqlQueries: DeleteSqlQueries,
    genealogyDatabaseService: GenealogyDatabaseService,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def deleteMediaConfirmation(baseId: Int, id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      (for {
        database <- OptionT(genealogyDatabaseService.getGenealogyDatabase(baseId))
        media    <- getSqlQueries.getMedia(baseId, id)
      } yield {
        Ok(deleteMediaView(Some(database), media))
      }).getOrElse(NotFound("Database or media not found"))
  }

  def deleteMediaAction(baseId: Int, id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      deleteSqlQueries.deleteMedia(id).map { _ =>
        Redirect(controllers.routes.HomeController.showSurnames(baseId))
      }
  }

}
