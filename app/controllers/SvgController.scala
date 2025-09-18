package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import actions.AuthAction
import models.AuthenticatedRequest
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import services.AscendanceService
import views.xml.pdfTemplates.SvgCompactTree

@Singleton
class SvgController @Inject() (
    authAction: AuthAction,
    ascendanceService: AscendanceService,
    svgCompactTree: SvgCompactTree,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def svg(id: Int): Action[AnyContent] = authAction.async { implicit request: AuthenticatedRequest[AnyContent] =>
    ascendanceService.buildSosaList(id).map { sosaList =>
      val title = sosaList.get(1).fold("") { person =>
        s"Ascendance of ${person.name}"
      }
      Ok(svgCompactTree(title, sosaList))
        .as("image/svg+xml")
        .withHeaders(
          "Content-Disposition" -> "inline; filename=diagram.svg"
        )
    }
  }
}
