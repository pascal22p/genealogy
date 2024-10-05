package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthAction
import models.forms.UserDataForm
import play.api.data.Form
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
class LoginController @Inject() (
    authAction: AuthAction,
    loginService: LoginService,
    sqlQueries: SessionSqlQueries,
    loginView: Login,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def onLoad: Action[AnyContent] = authAction.async { implicit authenticatedRequest =>
    Future.successful(Ok(loginView(UserDataForm.userForm)))
  }

  def onSubmit: Action[AnyContent] = authAction.async { implicit authenticatedRequest =>
    val errorFunction: Form[UserDataForm] => Future[Result] = { (formWithErrors: Form[UserDataForm]) =>
      // This is the bad case, where the form had validation errors.
      // Let's show the user the form again, with the errors highlighted.
      // Note how we pass the form with errors to the template.
      Future.successful(BadRequest(loginView(formWithErrors)))
    }

    val successFunction: UserDataForm => Future[Result] = { (userDataForm: UserDataForm) =>
      // This is the good case, where the form was successfully parsed as a Data object.
      loginService.getUserData(userDataForm.username, userDataForm.password).flatMap { resultOption =>
        resultOption.fold(Future.successful(Redirect(routes.LoginController.onLoad()))) { result =>
          val newLocalSession = authenticatedRequest.localSession
            .copy(sessionData = authenticatedRequest.localSession.sessionData.copy(userData = Some(result)))
          sqlQueries.updateSessionData(newLocalSession).map { _ =>
            Redirect(routes.HomeController.onload())
          }
        }
      }
    }

    val formValidationResult = UserDataForm.userForm.bindFromRequest()
    formValidationResult.fold(errorFunction, successFunction)
  }

}
