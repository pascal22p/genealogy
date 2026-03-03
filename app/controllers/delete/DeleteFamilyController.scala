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
import services.FamilyService
import services.GenealogyDatabaseService
import views.html.delete.DeleteFamilyView

@Singleton
class DeleteFamilyController @Inject() (
    authJourney: AuthJourney,
    familyService: FamilyService,
    genealogyDatabaseService: GenealogyDatabaseService,
    deleteFamilyView: DeleteFamilyView,
    deleteSqlQueries: DeleteSqlQueries,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def deleteFamilyConfirmation(baseId: Int, id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      (for {
        database <- OptionT(genealogyDatabaseService.getGenealogyDatabase(baseId))
        family   <- familyService.getFamilyDetails(id)
      } yield {
        Ok(deleteFamilyView(Some(database), family))
      }).getOrElse(NotFound("Database or family not found"))
  }

  def deleteFamilyAction(baseId: Int, id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      deleteSqlQueries
        .deleteFamily(id)
        .map { _ =>
          Redirect(controllers.routes.HomeController.showSurnames(baseId))
        }
  }

}
