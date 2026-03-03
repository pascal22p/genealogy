package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import actions.AuthAction
import cats.data.OptionT
import models.AuthenticatedRequest
import models.Family
import play.api.i18n.*
import play.api.mvc.*
import services.DescendanceService
import services.GenealogyDatabaseService
import services.SessionService
import views.html.Descendants

@Singleton
class DescendanceController @Inject() (
    authAction: AuthAction,
    genealogyDatabaseService: GenealogyDatabaseService,
    descendanceService: DescendanceService,
    sessionService: SessionService,
    descendantsView: Descendants,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def showDescendant(baseId: Int, id: Int): Action[AnyContent] = authAction.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      (for {
        database <- OptionT(genealogyDatabaseService.getGenealogyDatabase(baseId))
        tree     <- OptionT(descendanceService.getDescendant(id, 0))
      } yield {
        sessionService.insertPersonInHistory(tree.copy(families = List.empty[Family]))
        Ok(descendantsView(tree, Some(database)))
      }).getOrElse(NotFound("database or person not found"))
  }
}
