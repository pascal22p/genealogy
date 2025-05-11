package controllers.unlink

import javax.inject.*

import scala.concurrent.ExecutionContext

import actions.AuthJourney
import models.AuthenticatedRequest
import models.ResnType.PrivacyResn
import play.api.i18n.*
import play.api.mvc.*
import queries.DeleteSqlQueries
import services.FamilyService
import services.PersonService
import views.html.unlink.UnlinkChildView

@Singleton
class UnlinkChildController @Inject() (
    authJourney: AuthJourney,
    personService: PersonService,
    familyService: FamilyService,
    deleteSqlQueries: DeleteSqlQueries,
    unlinkChildView: UnlinkChildView,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def unlinkChildConfirmation(baseId: Int, childId: Int, familyId: Int): Action[AnyContent] =
    authJourney.authWithAdminRight.async { implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      for {
        familyOption <- familyService.getFamilyDetails(familyId)
        childOption  <- personService.getPerson(childId)
      } yield {
        (childOption, familyOption) match {
          case (Some(child), Some(family)) =>
            val isAllowedToSee = authenticatedRequest.localSession.sessionData.userData.fold(false)(_.seePrivacy)

            if (!child.details.privacyRestriction.contains(PrivacyResn) || isAllowedToSee) {
              Ok(unlinkChildView(child, family, baseId))
            } else {
              Forbidden("Not allowed")
            }

          case (_, _) => NotImplemented("Not implemented")
        }

      }
    }

  def unlinkChildAction(baseId: Int, childId: Int, familyId: Int): Action[AnyContent] =
    authJourney.authWithAdminRight.async { _ =>
      deleteSqlQueries.deleteChildFromFamily(childId, familyId).map { _ =>
        Redirect(controllers.routes.FamilyController.showFamily(baseId, familyId))
      }
    }

}
