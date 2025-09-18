package models

import models.Attrs
import org.slf4j.MarkerFactory
import play.api.mvc.RequestHeader
import play.api.Logging
import play.api.MarkerContext

trait LoggingWithRequest extends Logging {

  implicit def requestHeaderToMarkerContext(implicit request: RequestHeader): MarkerContext = {
    val requestId = request.attrs(Attrs.RequestId)
    val sessionId = request.attrs(Attrs.SessionId)
    val marker    = MarkerFactory.getDetachedMarker(s"requestId=$requestId, sessionId=$sessionId")
    MarkerContext(marker)
  }

}
