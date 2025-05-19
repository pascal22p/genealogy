package filters

import java.util.UUID
import javax.inject.Inject

import scala.concurrent.ExecutionContext

import org.apache.pekko.stream.Materializer
import play.api.mvc.*

class SessionFilter @Inject() (implicit val mat: Materializer, ec: ExecutionContext) extends EssentialFilter {

  private val SessionKey = "sessionId"

  override def apply(next: EssentialAction): EssentialAction = EssentialAction { requestHeader =>
    val hasSessionId = requestHeader.session.get(SessionKey).isDefined

    next(requestHeader).map { result =>
      if (hasSessionId) result
      else {
        val newSessionId = UUID.randomUUID().toString
        result.addingToSession(SessionKey -> newSessionId)(using requestHeader)
      }
    }
  }
}
