package controllers

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthAction
import models.forms.UserDataForm
import models.Session
import models.SessionData
import play.api.data.Form
import play.api.http.HeaderNames
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import play.api.mvc.Result
import queries.SessionSqlQueries
import services.LoginService
import views.html.Login

@Singleton
class SessionController @Inject() (
    authAction: AuthAction,
    loginService: LoginService,
    sqlQueries: SessionSqlQueries,
    loginView: Login,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def addNewSession(continueUrl: String): Action[AnyContent] = Action.async { implicit request =>
    val uri           = new java.net.URI(continueUrl)
    val path          = uri.getPath
    val query         = Option(uri.getQuery).getOrElse("")
    val pathWithQuery = if (query.nonEmpty) s"$path?$query" else path

    val uuid    = UUID.randomUUID().toString
    val session = Session(uuid, SessionData(None), LocalDateTime.now())
    sqlQueries.putSessionData(session).map(_ => Redirect(pathWithQuery).withSession("sessionId" -> uuid))
  }

  def loginOnLoad: Action[AnyContent] = authAction.async { implicit authenticatedRequest =>
    val returnUrl =
      authenticatedRequest.request.getQueryString("returnUrl").getOrElse(controllers.routes.HomeController.onload().url)
    Future.successful(Ok(loginView(UserDataForm.userForm.fill(UserDataForm("", "", returnUrl)))))
  }

  def loginOnSubmit: Action[AnyContent] = authAction.async { implicit authenticatedRequest =>
    val errorFunction: Form[UserDataForm] => Future[Result] = { (formWithErrors: Form[UserDataForm]) =>
      Future.successful(BadRequest(loginView(formWithErrors)))
    }

    val successFunction: UserDataForm => Future[Result] = { (userDataForm: UserDataForm) =>
      loginService.getUserData(userDataForm.username, userDataForm.password).flatMap { resultOption =>
        val returnUrl = new java.net.URI(
          Option(userDataForm.returnUrl).filter(_.trim.nonEmpty).getOrElse(routes.HomeController.onload().url)
        ).getPath
        resultOption.fold(Future.successful(Redirect(routes.SessionController.loginOnLoad()))) { result =>
          val newLocalSession = authenticatedRequest.localSession
            .copy(sessionData = authenticatedRequest.localSession.sessionData.copy(userData = Some(result)))
          sqlQueries.updateSessionData(newLocalSession).map {
            case 1 => Redirect(returnUrl)
            case _ => InternalServerError("Could not update session data")
          }
        }
      }
    }

    val formValidationResult = UserDataForm.userForm.bindFromRequest()
    formValidationResult.fold(errorFunction, successFunction)
  }

  def logoutOnLoad: Action[AnyContent] = authAction.async { implicit authenticatedRequest =>
    sqlQueries.removeSessionData(authenticatedRequest.localSession)
    val returnUrl =
      authenticatedRequest.request.headers.get(HeaderNames.REFERER).getOrElse(routes.HomeController.onload().url)
    Future.successful(Redirect(returnUrl))
  }
}
