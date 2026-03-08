package controllers

import javax.inject.*

import scala.concurrent.ExecutionContext

import actions.AuthAction
import cats.data.OptionT
import models.AuthenticatedRequest
import play.api.i18n.*
import play.api.mvc.*
import services.FamilyService
import services.GenealogyDatabaseService
import views.html.FamilyPage

@Singleton
class FamilyController @Inject() (
    authAction: AuthAction,
    genealogyDatabaseService: GenealogyDatabaseService,
    familyPage: FamilyPage,
    familyService: FamilyService,
    val controllerComponents: ControllerComponents,
)(implicit ec: ExecutionContext)
    extends BaseController
    with I18nSupport {

  def showFamily(baseId: Int, id: Int): Action[AnyContent] = authAction.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      (for {
        database <- OptionT(genealogyDatabaseService.getGenealogyDatabase(baseId))
        family   <- familyService.getFamilyDetails(id)
      } yield {
        Ok(familyPage(Some(database), family))
      }).getOrElse(NotFound("database or family not found"))
  }
}
