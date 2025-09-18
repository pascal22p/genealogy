package config

import javax.inject.Singleton

import scala.concurrent.*

import models.LoggingWithRequest
import play.api.http.HttpErrorHandler
import play.api.http.Status.NOT_FOUND
import play.api.mvc.*
import play.api.mvc.Results.*

@Singleton
class ErrorHandler extends HttpErrorHandler with LoggingWithRequest {
  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    if (statusCode == NOT_FOUND) {
      Future.successful(NotFound("Page not found"))
    } else {
      Future.successful(
        Status(statusCode)("A client error occurred: " + message)
      )
    }
  }

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    logger.error(exception.getMessage, exception)(using requestHeaderToMarkerContext(using request))
    Future.successful(
      InternalServerError("A server error occurred: " + exception.getMessage)
    )
  }
}
