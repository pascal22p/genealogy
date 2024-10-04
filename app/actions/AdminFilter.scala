package actions

import javax.inject.{Inject, Singleton}
import play.api.mvc.ActionFilter
import models.AuthenticatedRequest
import play.api.i18n.MessagesApi
import scala.concurrent.Future
import play.api.mvc.Result
import play.api.mvc.ControllerComponents
import views.html.ServiceUnavailable
import play.api.mvc.Results.Forbidden
import scala.concurrent.ExecutionContext
import play.api.i18n.I18nSupport

@Singleton
class AdminFilter @Inject()(
    serviceUnavailableView: ServiceUnavailable,
    cc: ControllerComponents
) extends ActionFilter[AuthenticatedRequest] with I18nSupport {

  override def messagesApi: MessagesApi = cc.messagesApi

  // scalastyle:off cyclomatic.complexity
  override def filter[A](request: AuthenticatedRequest[A]): Future[Option[Result]] = {
    implicit val implicitRequest: AuthenticatedRequest[A] = request

    val isAdmin: Boolean = request.localSession.sessionData.userData.map(_.isAdmin).getOrElse(false)

    if(isAdmin) {
        Future.successful(None)
    } else {
        Future.successful(Some(Forbidden(serviceUnavailableView("Not allowed"))))
    }
  }

  override protected implicit val executionContext: ExecutionContext = cc.executionContext

}
