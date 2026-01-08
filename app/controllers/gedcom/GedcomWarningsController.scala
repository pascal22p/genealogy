package controllers.gedcom

import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID
import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthJourney
import config.AppConfig
import models.forms.TrueOrFalseForm
import models.journeyCache.UserAnswersKey.*
import models.AuthenticatedRequest
import play.api.data.Form
import play.api.i18n.*
import play.api.mvc.*
import repositories.JourneyCacheRepository
import services.gedcom.GedcomCommonParser
import services.gedcom.GedcomImportService
import views.html.gedcom.GedcomWarningsView

@Singleton
class GedcomWarningsController @Inject() (
    authJourney: AuthJourney,
    journeyCacheRepository: JourneyCacheRepository,
    appConfig: AppConfig,
    gedcomCommonParser: GedcomCommonParser,
    gedcomImportService: GedcomImportService,
    gedcomWarningsView: GedcomWarningsView,
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController
    with I18nSupport {

  def showWarnings: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      journeyCacheRepository.get(ChooseGedcomFileQuestion).map {
        case None                 => Redirect(controllers.gedcom.routes.ImportGedcomController.startJourney)
        case Some(gedcomListForm) =>
          val basePath = Paths.get(appConfig.uploadPath)
          val sanitise = s"./${basePath.resolve(gedcomListForm.selectedFile).normalize()}"
          if (sanitise.startsWith(s"$basePath") && Files.exists(Paths.get(sanitise))) {
            val gedcomObject = gedcomCommonParser.getTree(sanitise)
            val jobId        = UUID.randomUUID().toString
            val warnings     = gedcomImportService.convertTree2SQLWarnings(gedcomObject.nodes, jobId)

            Ok(gedcomWarningsView(warnings.take(500).toSeq, TrueOrFalseForm.trueOrFalseForm))
          } else {
            InternalServerError("Gedcom file path should be valid")
          }
      }
  }

  def showWarningsOnSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      val errorFunction: Form[TrueOrFalseForm] => Future[Result] = { (_: Form[TrueOrFalseForm]) =>
        journeyCacheRepository.get(ChooseGedcomFileQuestion).map {
          case None                 => Redirect(controllers.gedcom.routes.ImportGedcomController.startJourney)
          case Some(gedcomListForm) =>
            val basePath = Paths.get(appConfig.uploadPath)
            val sanitise = s"./${basePath.resolve(gedcomListForm.selectedFile).normalize()}"
            if (sanitise.startsWith(s"$basePath") && Files.exists(Paths.get(sanitise))) {
              val gedcomObject = gedcomCommonParser.getTree(sanitise)
              val jobId        = UUID.randomUUID().toString
              val warnings     = gedcomImportService.convertTree2SQLWarnings(gedcomObject.nodes, jobId)

              BadRequest(gedcomWarningsView(warnings.take(500).toSeq, TrueOrFalseForm.trueOrFalseForm))
            } else {
              InternalServerError("Gedcom file path should be valid")
            }
        }
      }

      val successFunction: TrueOrFalseForm => Future[Result] = { (dataForm: TrueOrFalseForm) =>
        journeyCacheRepository
          .upsert(ClearExistingDatabaseQuestion, dataForm)
          .map { _ =>
            Redirect(controllers.gedcom.routes.CheckYourAnswersController.doGedcomImport)
          }
      }

      val formValidationResult = TrueOrFalseForm.trueOrFalseForm.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }

}
