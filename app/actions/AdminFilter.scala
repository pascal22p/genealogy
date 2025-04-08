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
import views.html.ServiceUnavailable

@Singleton
class AdminFilter @Inject() (
    serviceUnavailableView: ServiceUnavailable,
    cc: ControllerComponents
) extends ActionFilter[AuthenticatedRequest]
    with I18nSupport {

  override def messagesApi: MessagesApi = cc.messagesApi

  // scalastyle:off cyclomatic.complexity
  override def filter[A](request: AuthenticatedRequest[A]): Future[Option[Result]] = {
    implicit val implicitRequest: AuthenticatedRequest[A] = request

    val isAdmin: Boolean = request.localSession.sessionData.userData.exists(_.isAdmin)

    if (isAdmin) {
      Future.successful(None)
    } else {
      Future.successful(Some(Forbidden(serviceUnavailableView("Not allowed"))))
    }
  }

  protected implicit override val executionContext: ExecutionContext = cc.executionContext

}
