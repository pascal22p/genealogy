package controllers.delete

import javax.inject.*

import scala.concurrent.ExecutionContext

import actions.AuthJourney
import models.AuthenticatedRequest
import models.Person
import models.ResnType.PrivacyResn
import play.api.i18n.*
import play.api.mvc.*
import queries.DeleteSqlQueries
import services.PersonService
import views.html.delete.DeleteIndividual

@Singleton
class DeleteIndividualController @Inject() (
    authJourney: AuthJourney,
    personService: PersonService,
    deleteIndividualView: DeleteIndividual,
    deleteSqlQueries: DeleteSqlQueries,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def deletePersonConfirmation(baseId: Int, id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      personService.getPerson(id).map { (personOption: Option[Person]) =>
        personOption.fold(NotFound("Nothing here")) { person =>
          val isAllowedToSee = authenticatedRequest.localSession.sessionData.userData.fold(false)(_.seePrivacy)

          if (!person.details.privacyRestriction.contains(PrivacyResn) || isAllowedToSee) {
            Ok(deleteIndividualView(person, baseId))
          } else {
            Forbidden("Not allowed")
          }
        }
      }
  }

  def deletePersonAction(baseId: Int, id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      personService.getPerson(id).map { (personOption: Option[Person]) =>
        personOption.fold(NotFound("Nothing here")) { person =>
          val isAllowedToSee = authenticatedRequest.localSession.sessionData.userData.fold(false)(_.seePrivacy)

          if (!person.details.privacyRestriction.contains(PrivacyResn) || isAllowedToSee) {
            deleteSqlQueries.deletePersonDetails(person.details.id)
            Redirect(controllers.routes.HomeController.showSurnames(baseId))
          } else {
            Forbidden("Not allowed")
          }
        }
      }
  }

}
