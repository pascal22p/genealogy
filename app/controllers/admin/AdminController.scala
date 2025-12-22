package controllers.admin

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.Future

import actions.AuthJourney
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import views.html.admin.AdminIndexView

@Singleton
class AdminController @Inject() (
    authJourney: AuthJourney,
    adminIndexView: AdminIndexView,
    val controllerComponents: ControllerComponents
)() extends BaseController
    with I18nSupport {

  def index: Action[AnyContent] = authJourney.authWithAdminRight.async { implicit request =>
    Future.successful(Ok(adminIndexView()))
  }
}
