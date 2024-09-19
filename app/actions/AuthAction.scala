package actions

import java.util.UUID
import javax.inject.Inject

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.google.inject.ImplementedBy
import models.AuthenticatedRequest
import models.Session
import models.SessionData
import play.api.mvc.ActionBuilder
import play.api.mvc.ActionFunction
import play.api.mvc.AnyContent
import play.api.mvc.BodyParser
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import queries.MariadbQueries

class AuthActionImpl @Inject() (
    mariadbQueries: MariadbQueries,
    cc: MessagesControllerComponents,
)(implicit val ec: ExecutionContext)
    extends AuthAction {

  override val parser: BodyParser[AnyContent]               = cc.parsers.defaultBodyParser
  protected override val executionContext: ExecutionContext = cc.executionContext

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    @SuppressWarnings(Array("org.wartremover.warts.ToString"))
    val uuid      = UUID.randomUUID().toString
    val sessionId = request.session.get("sessionId").getOrElse(uuid)
    mariadbQueries.getSessionData(sessionId).flatMap {
      case Some(session) =>
        mariadbQueries.sessionKeepAlive(sessionId).flatMap { _ =>
          block {
            AuthenticatedRequest(
              request,
              session
            )
          }
        }
      case None =>
        val session = Session(sessionId, SessionData(1, None))
        mariadbQueries.putSessionData(session).map(_ => Redirect(request.uri).withSession("sessionId" -> sessionId))
    }
  }

}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction
    extends ActionBuilder[AuthenticatedRequest, AnyContent]
    with ActionFunction[Request, AuthenticatedRequest]
