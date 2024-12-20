package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthAction
import models.forms.UserDataForm
import play.api.data.Form
import play.api.http.HeaderNames
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import queries.SessionSqlQueries
import services.LoginService
import views.html.Login

@Singleton
class LogoutController @Inject() (
    authAction: AuthAction,
    sqlQueries: SessionSqlQueries,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def onLoad: Action[AnyContent] = authAction.async { implicit authenticatedRequest =>
    sqlQueries.removeSessionData(authenticatedRequest.localSession)
    val returnUrl =
      authenticatedRequest.request.headers.get(HeaderNames.REFERER).getOrElse(routes.HomeController.onload().url)
    Future.successful(Redirect(returnUrl))
  }

}
