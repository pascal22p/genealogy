package actions

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.google.inject.ImplementedBy
import models.AuthenticatedRequest
import models.GenealogyDatabase
import models.LoggingWithRequest
import models.Session
import models.SessionData
import play.api.mvc.ActionBuilder
import play.api.mvc.ActionFunction
import play.api.mvc.AnyContent
import play.api.mvc.BodyParser
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.MarkerContext
import queries.GetSqlQueries
import queries.SessionSqlQueries

class AuthActionImpl @Inject() (
    sqlQueries: SessionSqlQueries,
    getSqlQueries: GetSqlQueries,
    cc: MessagesControllerComponents,
)(implicit val ec: ExecutionContext)
    extends AuthAction
    with LoggingWithRequest {

  override val parser: BodyParser[AnyContent]               = cc.parsers.defaultBodyParser
  protected override val executionContext: ExecutionContext = cc.executionContext

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    @SuppressWarnings(Array("org.wartremover.warts.ToString"))
    val uuid      = UUID.randomUUID().toString
    val sessionId = request.session.get("sessionId").getOrElse(uuid)
    val baseRegex = """^/base/([0-9]+)/.*""".r

    logger.info(s"AuthAction with session ID: $sessionId")

    val databaseFuture: Future[Option[GenealogyDatabase]] = request.path match {
      case baseRegex(baseId) =>
        getSqlQueries.getGenealogyDatabase(baseId.toInt).value
      case _ =>
        Future.successful(None)
    }

    databaseFuture.flatMap { database =>
      sqlQueries.getSessionData(sessionId).flatMap {
        case Some(session) =>
          sqlQueries.sessionKeepAlive(sessionId).flatMap { _ =>
            block {
              AuthenticatedRequest(
                request,
                session,
                database
              )
            }
          }
        case None =>
          val session = Session(sessionId, SessionData(None), LocalDateTime.now())
          sqlQueries.putSessionData(session).flatMap { _ =>
            block {
              AuthenticatedRequest(
                request,
                session,
                database
              )
            }
          }
      }

    }
  }

}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction
    extends ActionBuilder[AuthenticatedRequest, AnyContent]
    with ActionFunction[Request, AuthenticatedRequest]
