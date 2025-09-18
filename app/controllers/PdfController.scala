package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import actions.AuthAction
import models.AuthenticatedRequest
import models.LoggingWithRequest
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import services.AscendanceService
import services.FopService

@Singleton
class PdfController @Inject() (
    authAction: AuthAction,
    fopService: FopService,
    ascendanceService: AscendanceService,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport
    with LoggingWithRequest {

  def pdf(id: Int): Action[AnyContent] = authAction.async { implicit request: AuthenticatedRequest[AnyContent] =>
    ascendanceService.buildSosaList(id).map { sosaList =>
      try {
        val pdfBytes = fopService.xmlTopdf(sosaList)
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
