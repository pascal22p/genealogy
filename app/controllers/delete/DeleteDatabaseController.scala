package controllers.delete

import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthJourney
import models.AuthenticatedRequest
import play.api.i18n.*
import play.api.mvc.*
import queries.DeleteSqlQueries
import queries.GetSqlQueries
import views.html.delete.DeleteDatabase

@Singleton
class DeleteDatabaseController @Inject() (
    authJourney: AuthJourney,
    deleteDatabaseView: DeleteDatabase,
    getSqlQueries: GetSqlQueries,
    deleteSqlQueries: DeleteSqlQueries,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def deleteDatabaseConfirmation(id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      getSqlQueries.getGenealogyDatabase(id).fold(NotFound("database not found")) { database =>
        val isAllowedToEdit = authenticatedRequest.localSession.sessionData.userData.fold(false)(_.seePrivacy)

        if (isAllowedToEdit) {
          Ok(deleteDatabaseView(database))
        } else {
          Forbidden("Not allowed")
        }
      }
  }

  def deleteDatabaseAction(id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      getSqlQueries
        .getGenealogyDatabase(id)
        .fold(Future.successful(NotFound("database not found"))) { database =>
          val isAllowedToEdit = authenticatedRequest.localSession.sessionData.userData.fold(false)(_.seePrivacy)

          if (isAllowedToEdit) {
            deleteSqlQueries.deleteGenealogyDatabase(id).map { _ =>
              Redirect(controllers.routes.HomeController.onload())
            }
          } else {
            Future.successful(Forbidden("Not allowed"))
          }
        }
        .flatten
  }

}
