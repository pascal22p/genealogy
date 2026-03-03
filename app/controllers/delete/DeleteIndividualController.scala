package controllers.delete

import javax.inject.*

import scala.concurrent.ExecutionContext

import actions.AuthJourney
import cats.data.OptionT
import cats.implicits.*
import models.AuthenticatedRequest
import models.ResnType.PrivacyResn
import play.api.i18n.*
import play.api.mvc.*
import queries.DeleteSqlQueries
import services.FamilyService
import services.GenealogyDatabaseService
import services.PersonDetailsService
import services.PersonService
import views.html.delete.DeleteIndividual

@Singleton
class DeleteIndividualController @Inject() (
    authJourney: AuthJourney,
    personService: PersonService,
    familyService: FamilyService,
    personDetailsService: PersonDetailsService,
    genealogyDatabaseService: GenealogyDatabaseService,
    deleteIndividualView: DeleteIndividual,
    deleteSqlQueries: DeleteSqlQueries,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def deletePersonConfirmation(baseId: Int, id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      (for {
        database           <- OptionT(genealogyDatabaseService.getGenealogyDatabase(baseId))
        familiesIdPartners <- OptionT.liftF(familyService.getFamilyIdsFromPartnerId(id))
        families           <- OptionT.liftF(familiesIdPartners.traverse(id => familyService.getFamilyDetails(id).value))
        parents            <- OptionT.liftF(personDetailsService.getParents(id))
        person             <- OptionT(personService.getPerson(id))
      } yield {
        val isAllowedToSee = authenticatedRequest.localSession.sessionData.userData.fold(false)(_.seePrivacy)

        if (!person.details.privacyRestriction.contains(PrivacyResn) || isAllowedToSee) {
          Ok(deleteIndividualView(person, families.flatten, parents, Some(database)))
        } else {
          Forbidden("Not allowed")
        }
      }).getOrElse(NotFound("Database or person not found"))
  }

  def deletePersonAction(baseId: Int, id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      deleteSqlQueries.deletePersonDetails(id).map { _ =>
        Redirect(controllers.routes.HomeController.showSurnames(baseId))
      }
  }

}
