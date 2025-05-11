package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.Future

import actions.AuthAction
import play.api.http.HeaderNames
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import queries.SessionSqlQueries

@Singleton
class LogoutController @Inject() (
    authAction: AuthAction,
    sqlQueries: SessionSqlQueries,
    val controllerComponents: ControllerComponents
) extends BaseController
    with I18nSupport {

  def onLoad: Action[AnyContent] = authAction.async { implicit authenticatedRequest =>
    sqlQueries.removeSessionData(authenticatedRequest.localSession)
    val returnUrl =
      authenticatedRequest.request.headers.get(HeaderNames.REFERER).getOrElse(routes.HomeController.onload().url)
    Future.successful(Redirect(returnUrl))
  }

}
