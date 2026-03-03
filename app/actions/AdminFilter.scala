package actions

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import models.AuthenticatedRequest
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc.ActionFilter
import play.api.mvc.ControllerComponents
import play.api.mvc.Result
import play.api.mvc.Results.Redirect

@Singleton
class AdminFilter @Inject() (
    cc: ControllerComponents
) extends ActionFilter[AuthenticatedRequest]
    with I18nSupport {

  override def messagesApi: MessagesApi = cc.messagesApi

  // scalastyle:off cyclomatic.complexity
  override def filter[A](request: AuthenticatedRequest[A]): Future[Option[Result]] = {
    val isAdmin: Boolean = request.localSession.sessionData.userData.exists(_.isAdmin)

    if (isAdmin) {
      Future.successful(None)
    } else {
      Future.successful(Some(Redirect(controllers.routes.SessionController.loginOnLoad(request.uri))))
    }
  }

  protected implicit override val executionContext: ExecutionContext = cc.executionContext

}
