package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthAction
import models.AuthenticatedRequest
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import play.api.Logging
import services.FopService

@Singleton
class PdfController @Inject() (
    authAction: AuthAction,
    fopService: FopService,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport
    with Logging {

  def pdf(): Action[AnyContent] = authAction.async { implicit request: AuthenticatedRequest[AnyContent] =>
    Future {
      try {
        val pdfBytes = fopService.xmlTopdf()
        // Send PDF to browser
        Ok(pdfBytes)
          .as("application/pdf")
          .withHeaders(
            "Content-Disposition" -> "inline; filename=diagram.pdf"
          )
      } catch {
        case e: Exception =>
          logger.error(e.getMessage, e)
          InternalServerError(s"Error generating PDF: ${e.getMessage}")
      }
    }
  }
}
