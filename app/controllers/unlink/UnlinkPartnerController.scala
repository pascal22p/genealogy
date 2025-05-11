package controllers.unlink

import javax.inject.*

import scala.concurrent.ExecutionContext

import actions.AuthJourney
import models.AuthenticatedRequest
import models.ResnType.PrivacyResn
import play.api.i18n.*
import play.api.mvc.*
import queries.UpdateSqlQueries
import services.FamilyService
import services.PersonService
import views.html.unlink.UnlinkPartnerView

@Singleton
class UnlinkPartnerController @Inject() (
    authJourney: AuthJourney,
    personService: PersonService,
    familyService: FamilyService,
    updateSqlQueries: UpdateSqlQueries,
    unlinkPartnerView: UnlinkPartnerView,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def unlinkPartnerConfirmation(baseId: Int, partnerId: Int, familyId: Int): Action[AnyContent] =
    authJourney.authWithAdminRight.async { implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      for {
        familyOption  <- familyService.getFamilyDetails(familyId)
        partnerOption <- personService.getPerson(partnerId)
      } yield {
        (partnerOption, familyOption) match {
          case (Some(partner), Some(family)) =>
            val isAllowedToSee = authenticatedRequest.localSession.sessionData.userData.fold(false)(_.seePrivacy)

            if (!partner.details.privacyRestriction.contains(PrivacyResn) || isAllowedToSee) {
              Ok(unlinkPartnerView(partner, family, baseId))
            } else {
              Forbidden("Not allowed")
            }

          case (_, _) => NotImplemented("Not implemented")
        }

      }
    }

  def unlinkPartnerAction(baseId: Int, partnerId: Int, familyId: Int): Action[AnyContent] =
    authJourney.authWithAdminRight.async { _ =>
      updateSqlQueries.deletePartnerFromFamily(partnerId, familyId).map { _ =>
        Redirect(controllers.routes.FamilyController.showFamily(baseId, familyId))
      }
    }

}
