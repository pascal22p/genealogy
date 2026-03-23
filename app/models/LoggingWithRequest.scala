package models

import models.Attrs
import org.slf4j.MDC
import org.slf4j.MarkerFactory
import play.api.mvc.RequestHeader
import play.api.Logging
import play.api.MarkerContext

trait LoggingWithRequest extends Logging {

  @SuppressWarnings(Array("org.wartremover.warts.RedundantConversions"))
  implicit def requestHeaderToMarkerContext(implicit request: RequestHeader): MarkerContext = {
    val requestId = request.attrs(Attrs.RequestId)
    val sessionId = request.attrs(Attrs.SessionId)

    // Set on MDC so all loggers pick it up
    MDC.put("request_id", requestId.toString)
    MDC.put("session_id", sessionId.toString)

    val marker = MarkerFactory.getDetachedMarker(s"requestId=$requestId, sessionId=$sessionId")
    MarkerContext(marker)
  }

  def withRequestLogging[A](block: => A)(implicit request: RequestHeader): A = {
    requestHeaderToMarkerContext
    try block
    finally {
      MDC.remove("request_id")
      MDC.remove("session_id")
    }
  }

}
