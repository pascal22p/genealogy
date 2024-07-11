package actions

import javax.inject.Inject

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.google.inject.ImplementedBy
import models.AuthenticatedRequest
import play.api.mvc.ActionBuilder
import play.api.mvc.ActionFunction
import play.api.mvc.AnyContent
import play.api.mvc.BodyParser
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Request
import play.api.mvc.Result

class AuthActionImpl @Inject() (
    cc: MessagesControllerComponents,
)(implicit val ec: ExecutionContext)
    extends AuthAction {

  override val parser: BodyParser[AnyContent]               = cc.parsers.defaultBodyParser
  protected override val executionContext: ExecutionContext = cc.executionContext

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    block {
      AuthenticatedRequest(
        request,
        1
      )
    }
  }

}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction
    extends ActionBuilder[AuthenticatedRequest, AnyContent]
    with ActionFunction[Request, AuthenticatedRequest]
