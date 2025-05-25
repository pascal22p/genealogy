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
import views.html.delete.DeleteIndividual

@Singleton
class DeleteIndividualController @Inject() (
    authJourney: AuthJourney,
    personService: PersonService,
    familyService: FamilyService,
    personDetailsService: PersonDetailsService,
    deleteIndividualView: DeleteIndividual,
    deleteSqlQueries: DeleteSqlQueries,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def deletePersonConfirmation(baseId: Int, id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      for {
        familiesIdPartners <- familyService.getFamilyIdsFromPartnerId(id)
        families           <- familiesIdPartners.traverse(id => familyService.getFamilyDetails(id).value)
        parents            <- personDetailsService.getParents(id)
        personOption       <- personService.getPerson(id)
      } yield {
        personOption.fold(NotFound("Nothing here")) { person =>
          val isAllowedToSee = authenticatedRequest.localSession.sessionData.userData.fold(false)(_.seePrivacy)

          if (!person.details.privacyRestriction.contains(PrivacyResn) || isAllowedToSee) {
            Ok(deleteIndividualView(person, families.flatten, parents, baseId))
          } else {
            Forbidden("Not allowed")
          }
        }
      }
  }

  def deletePersonAction(baseId: Int, id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      deleteSqlQueries.deletePersonDetails(id).map { _ =>
        Redirect(controllers.routes.HomeController.showSurnames(baseId))
      }
  }

}
