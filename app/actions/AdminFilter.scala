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
import play.api.mvc.Results.Forbidden
import play.api.mvc.Results.Redirect

@Singleton
class AdminFilter @Inject() (
    cc: ControllerComponents
) extends ActionFilter[AuthenticatedRequest]
    with I18nSupport {

  override def messagesApi: MessagesApi = cc.messagesApi

  // scalastyle:off cyclomatic.complexity
  override def filter[A](request: AuthenticatedRequest[A]): Future[Option[Result]] = Future {
    request.localSession.sessionData.userData.map(_.isAdmin) match {
      // not authenticated
      case None => Some(Redirect(controllers.routes.SessionController.loginOnLoad(request.uri)))
      // authenticated but not an admin
      case Some(false) => Some(Forbidden("Current credential is not an admin"))
      // authenticate and an admin
      case Some(true) => None
    }
  }

  protected implicit override val executionContext: ExecutionContext = cc.executionContext

}
