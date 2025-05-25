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
import services.FamilyService
import services.PersonDetailsService
import services.PersonService
import views.html.delete.DeleteFamilyView

@Singleton
class DeleteFamilyController @Inject() (
    authJourney: AuthJourney,
    personService: PersonService,
    familyService: FamilyService,
    personDetailsService: PersonDetailsService,
    deleteFamilyView: DeleteFamilyView,
    deleteSqlQueries: DeleteSqlQueries,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def deleteFamilyConfirmation(baseId: Int, id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      familyService.getFamilyDetails(id).fold(NotFound("Nothing here")) { family =>
        Ok(deleteFamilyView(baseId, family))
      }
  }

  def deleteFamilyAction(baseId: Int, id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      deleteSqlQueries
        .deleteFamily(id)
        .map { _ =>
          Redirect(controllers.routes.HomeController.showSurnames(baseId))
        }
        .recover {
          case ex =>
            println(s"OOOOOO  + ${ex.getClass}")
            throw ex
        }
  }

}
