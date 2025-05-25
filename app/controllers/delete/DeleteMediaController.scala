package controllers.delete

import javax.inject.*

import scala.concurrent.ExecutionContext

import actions.AuthJourney
import cats.implicits.*
import models.AuthenticatedRequest
import models.Person
import models.ResnType.PrivacyResn
import play.api.i18n.*
import play.api.mvc.*
import queries.DeleteSqlQueries
import queries.GetSqlQueries
import services.FamilyService
import services.PersonDetailsService
import services.PersonService
import views.html.delete.DeleteMediaView

@Singleton
class DeleteMediaController @Inject() (
    authJourney: AuthJourney,
    deleteMediaView: DeleteMediaView,
    getSqlQueries: GetSqlQueries,
    deleteSqlQueries: DeleteSqlQueries,
    familyService: FamilyService,
    personDetailsService: PersonDetailsService,
    personService: PersonService,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def deleteMediaConfirmation(baseId: Int, id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      getSqlQueries.getMedia(baseId, id).fold(NotFound("Nothing here")) { media =>
        Ok(deleteMediaView(baseId, media))
      }
  }

  def deleteMediaAction(baseId: Int, id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      deleteSqlQueries.deleteMedia(id).map { _ =>
        Redirect(controllers.routes.HomeController.showSurnames(baseId))
      }
  }

}
