package controllers

import javax.inject.*

import scala.concurrent.ExecutionContext

import actions.AuthAction
import models.AuthenticatedRequest
import play.api.i18n.*
import play.api.mvc.*
import services.FamilyService
import views.html.FamilyPage

@Singleton
class FamilyController @Inject() (
    authAction: AuthAction,
    familyPage: FamilyPage,
    familyService: FamilyService,
    val controllerComponents: ControllerComponents,
)(implicit ec: ExecutionContext)
    extends BaseController
    with I18nSupport {

  def showFamily(baseId: Int, id: Int): Action[AnyContent] = authAction.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      familyService.getFamilyDetails(id).map { familyOption =>
        familyOption.fold(NotFound("Family cannot be found")) { family =>
          Ok(familyPage(baseId, family))
        }
      }
  }
}
