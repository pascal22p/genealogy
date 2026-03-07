package controllers.admin

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthJourney
import models.AuthenticatedRequest
import models.GenealogyDatabase
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import play.api.mvc.Result
import services.GenealogyDatabaseService
import views.html.admin.AdminIndexView

@Singleton
class AdminController @Inject() (
    authJourney: AuthJourney,
    genealogyDatabaseService: GenealogyDatabaseService,
    adminIndexView: AdminIndexView,
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController
    with I18nSupport {

  def indexNoBase: Action[AnyContent] = authJourney.authWithAdminRight.async { implicit request =>
    indexAction(None)
  }

  def index(dbId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async { implicit request =>
    indexAction(Some(dbId))
  }

  private def indexAction(dbId: Option[Int])(implicit authenticatedRequest: AuthenticatedRequest[?]): Future[Result] = {
    dbId
      .fold(Future.successful(Option.empty[GenealogyDatabase]))(genealogyDatabaseService.getGenealogyDatabase)
      .map(database => Ok(adminIndexView(database)))
  }
}
