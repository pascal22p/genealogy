package config

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.*

import models.LoggingWithRequest
import play.api.http.HttpErrorHandler
import play.api.http.Status.NOT_FOUND
import play.api.mvc.*
import play.api.mvc.Results.*
import play.api.Environment

@Singleton
class ErrorHandler @Inject() (env: Environment) extends HttpErrorHandler with LoggingWithRequest {
  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    if (statusCode == NOT_FOUND) {
      Future.successful(NotFound("Page not found"))
    } else {
      Future.successful(
        Status(statusCode)("A client error occurred: " + message)
      )
    }
  }

  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    logger.error(exception.getMessage, exception)(using requestHeaderToMarkerContext(using request))

    env.mode.toString match {
      case "Dev" =>
        Future.successful(InternalServerError("A server error occurred: " + exception.getMessage))
      case _ =>
        Future.successful(InternalServerError(s"A server error occurred. Request id: ${request.id}"))
    }
  }
}
