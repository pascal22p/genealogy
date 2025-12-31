package controllers.gedcom

import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.*

import scala.concurrent.ExecutionContext

import actions.AuthJourney
import config.AppConfig
import models.journeyCache.UserAnswersKey.*
import models.AuthenticatedRequest
import play.api.i18n.*
import play.api.mvc.*
import repositories.JourneyCacheRepository
import services.gedcom.GedcomCommonParser
import services.gedcom.GedcomImportService
import views.html.gedcom.GedcomStatsView

@Singleton
class GedcomStatsController @Inject() (
    authJourney: AuthJourney,
    journeyCacheRepository: JourneyCacheRepository,
    appConfig: AppConfig,
    gedcomCommonParser: GedcomCommonParser,
    gedcomImportService: GedcomImportService,
    gedcomStatsView: GedcomStatsView,
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController
    with I18nSupport {

  def gedcomStats: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      journeyCacheRepository.get(ChooseGedcomFileQuestion).map {
        case None                 => Redirect(controllers.gedcom.routes.ImportGedcomController.startJourney)
        case Some(gedcomListForm) =>
          val basePath = Paths.get(appConfig.uploadPath)
          val sanitise = s"./${basePath.resolve(gedcomListForm.selectedFile).normalize()}"
          if (sanitise.startsWith(s"$basePath") && Files.exists(Paths.get(sanitise))) {
            val gedcomObject = gedcomCommonParser.getTree(sanitise)
            val warnings     = gedcomImportService.convertTree2SQLWarnings(gedcomObject.nodes)

            Ok(gedcomStatsView(gedcomObject, warnings.take(100)))
          } else {
            InternalServerError("Gedcom file path should be valid")
          }
      }
  }
}
