package controllers.delete

import javax.inject.*
import scala.concurrent.ExecutionContext
import actions.AuthJourney
import cats.implicits.*
import models.AuthenticatedRequest
import play.api.i18n.*
import play.api.mvc.*
import queries.DeleteSqlQueries
import services.SourCitationService
import views.html.delete.DeleteSourCitationView

@Singleton
class DeleteSourCitationController @Inject() (
    authJourney: AuthJourney,
    deleteSourCitationView: DeleteSourCitationView,
    deleteSqlQueries: DeleteSqlQueries,
    sourCitationService: SourCitationService,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def deleteSourCitationConfirmation(baseId: Int, id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      sourCitationService.getSourCitation(id, baseId).fold(NotFound("Nothing here")) { sourCitation =>
        Ok(deleteSourCitationView(baseId, sourCitation))
      }
  }

  def deleteSourCitationAction(baseId: Int, id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      deleteSqlQueries.deleteSourCitation(id).map { _ =>
        Redirect(controllers.routes.HomeController.showSurnames(baseId))
      }
  }

}
