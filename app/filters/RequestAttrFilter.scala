package filters

import java.util.UUID
import javax.inject.Inject

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import models.Attrs
import org.apache.pekko.stream.Materializer
import play.api.mvc.*
import play.api.Logging
import config.ErrorHandler

class RequestAttrFilter @Inject() (
    errorHandler: ErrorHandler,
    implicit val mat: Materializer
)(implicit ec: ExecutionContext)
    extends Filter
    with Logging {

  private val headerName = "X-Request-Id"

  override def apply(next: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] = {
    val requestId = request.headers.get(headerName).getOrElse(UUID.randomUUID().toString)
    val sessionId = request.session.get("sessionId").getOrElse("no-session")

    // Attach requestId into attrs (server-side context)
    val enrichedRequest = request
      .addAttr(Attrs.RequestId, requestId)
      .addAttr(Attrs.SessionId, sessionId)

    logger.info(
      s"RequestAttrFilter assigned sessionId=`$sessionId` requestId=`$requestId` to ${request.method} ${request.uri}"
    )

    // Also put it on the response headers (optional, useful for clients/debugging)
    next(enrichedRequest)
      .map { req =>
        val newHeaders = (req.header.headers - headerName) + (headerName -> requestId)
        req.withHeaders(newHeaders.toSeq*)
      }
      .recoverWith {
        case NonFatal(e) => errorHandler.onServerError(enrichedRequest, e)
      }
  }
}
