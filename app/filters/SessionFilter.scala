package filters

import java.util.UUID
import javax.inject.Inject

import scala.concurrent.ExecutionContext

import play.api.mvc.*
import org.apache.pekko.stream.Materializer
import models.Attrs

class SessionFilter @Inject() (
    implicit val mat: Materializer,
    ec: ExecutionContext
) extends EssentialFilter {

  private val SessionKey = "sessionId"

  override def apply(next: EssentialAction): EssentialAction =
    EssentialAction { requestHeader =>
      val (sessionId, isNew) =
        requestHeader.session.get(SessionKey) match {
          case Some(existingSessionId) =>
            existingSessionId -> false

          case None =>
            UUID.randomUUID().toString -> true
        }

      val requestWithSessionId =
        requestHeader.addAttr(Attrs.SessionId, sessionId)

      next(requestWithSessionId).map { result =>
        if (isNew) {
          result.addingToSession(SessionKey -> sessionId)(using requestHeader)
        } else {
          result
        }
      }
    }

  def sessionId(request: RequestHeader): Option[String] =
    request.attrs.get(Attrs.SessionId)
}
