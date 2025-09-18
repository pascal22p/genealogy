package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.annotation.unused
import scala.concurrent.ExecutionContext

import actions.AuthAction
import models.AuthenticatedRequest
import models.Family
import play.api.i18n.*
import play.api.mvc.*
import services.DescendanceService
import services.SessionService
import views.html.Descendants

@Singleton
class DescendanceController @Inject() (
    authAction: AuthAction,
    descendanceService: DescendanceService,
    sessionService: SessionService,
    descendantsView: Descendants,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def showDescendant(@unused baseId: Int, id: Int): Action[AnyContent] = authAction.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      descendanceService.getDescendant(id, 0).map {
        case Some(tree) =>
          sessionService.insertPersonInHistory(tree.copy(families = List.empty[Family]))
          Ok(descendantsView(tree, 1))
        case None => NotFound("Nothing here")
      }
  }
}
