package controllers.gedcom

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.jdk.CollectionConverters.IteratorHasAsScala

import actions.AuthJourney
import models.AuthenticatedRequest
import play.api.data.Form
import play.api.i18n.*
import play.api.mvc.*
import play.api.mvc.AnyContent
import config.AppConfig
import models.forms.extensions.FillFormExtension.filledWith
import models.forms.DatabaseForm
import models.forms.GedcomListForm
import models.forms.TrueOrFalseForm
import models.journeyCache.*
import models.journeyCache.GedcomPath
import repositories.JourneyCacheRepository
import services.gedcom.GedcomCommonParser
import services.gedcom.GedcomImportService
import utils.ReadFile
import views.html.add.AddDatabase
import views.html.gedcom.CheckYourAnswersView
import views.html.gedcom.GedcomListView
import views.html.gedcom.GedcomStatsView
import views.html.gedcom.NewDatabaseQuestionView

@Singleton
class ImportGedcomController @Inject() (
    authJourney: AuthJourney,
    journeyCacheRepository: JourneyCacheRepository,
    appConfig: AppConfig,
    readFile: ReadFile,
    gedcomCommonParser: GedcomCommonParser,
    gedcomImportService: GedcomImportService,
    gedcomListView: GedcomListView,
    gedcomStatsView: GedcomStatsView,
    newDatabaseQuestionView: NewDatabaseQuestionView,
    addDatabaseView: AddDatabase,
    checkYourAnswersView: CheckYourAnswersView,
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController
    with I18nSupport {

  private def listGedcomFiles: List[String] = {
    val folderPath = Paths.get(appConfig.uploadPath)
    val matcher    = FileSystems.getDefault.getPathMatcher("glob:**/*.ged")

    Files
      .walk(folderPath)
      .iterator()
      .asScala
      .filter(Files.isRegularFile(_))
      .filter(path => matcher.matches(folderPath.relativize(path)))
      .toList
      .map(_.toString)
  }

  private lazy val newDatabaseOnSubmitLink: Call =
    controllers.gedcom.routes.ImportGedcomController.addNewDatabaseOnSubmit

  def startJourney: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      Future.successful(Redirect(controllers.gedcom.routes.ImportGedcomController.showGedcomList))
  }

  def showGedcomList: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      journeyCacheRepository.get(GedcomPath).map { defaults =>
        val form = GedcomListForm.form(appConfig.uploadPath).filledWith(defaults)
        Ok(gedcomListView(form, listGedcomFiles))
      }
  }

  def gedcomListOnSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      val errorFunction: Form[GedcomListForm] => Future[Result] = { (formWithErrors: Form[GedcomListForm]) =>
        Future.successful(BadRequest(gedcomListView(formWithErrors, listGedcomFiles)))
      }

      val successFunction: GedcomListForm => Future[Result] = { (dataForm: GedcomListForm) =>
        journeyCacheRepository.upsert(GedcomPath, dataForm).map { _ =>
          Redirect(controllers.gedcom.routes.ImportGedcomController.gedcomStats)
        }
      }

      val formValidationResult = GedcomListForm.form(appConfig.uploadPath).bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }

  def gedcomStats: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      journeyCacheRepository.get(GedcomPath).map {
        case None                 => Redirect(controllers.gedcom.routes.ImportGedcomController.startJourney)
        case Some(gedcomListForm) =>
          val basePath = Paths.get(appConfig.uploadPath)
          val sanitise = s"./${basePath.resolve(gedcomListForm.selectedFile).normalize()}"
          if (sanitise.startsWith(s"$basePath") && Files.exists(Paths.get(sanitise))) {
            val gedcomTxt = readFile.asString(sanitise)
            val nodes     = gedcomCommonParser.getTree(gedcomTxt)
            val sqls      = gedcomImportService.convertTree2SQL(nodes, 4)

            Ok(gedcomStatsView(sqls.swap.getOrElse(List.empty)))
          } else {
            InternalServerError("Gedcom file path should be valid")
          }
      }
  }

  def showNewDatabaseQuestion: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      journeyCacheRepository.get(NewDatabaseQuestion).map { defaults =>
        val form = TrueOrFalseForm.trueOrFalseForm.filledWith(defaults)
        Ok(newDatabaseQuestionView(form))
      }
  }

  def newDatabaseQuestionOnSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      val errorFunction: Form[TrueOrFalseForm] => Future[Result] = { (formWithErrors: Form[TrueOrFalseForm]) =>
        Future.successful(BadRequest(newDatabaseQuestionView(formWithErrors)))
      }

      val successFunction: TrueOrFalseForm => Future[Result] = { (dataForm: TrueOrFalseForm) =>
        journeyCacheRepository
          .upsert(NewDatabaseQuestion, dataForm)
          .map { _ =>
            if (dataForm.trueOrFalse) {
              Redirect(controllers.gedcom.routes.ImportGedcomController.addNewDatabase)
            } else {
              Redirect(controllers.gedcom.routes.ImportGedcomController.checkYourAnswersImportGedcom)
            }
          }
      }

      val formValidationResult = TrueOrFalseForm.trueOrFalseForm.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }

  def addNewDatabase: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      journeyCacheRepository.get(NewDatabase).map { defaults =>
        val form = DatabaseForm.databaseForm.filledWith(defaults)
        Ok(addDatabaseView(form, newDatabaseOnSubmitLink))
      }
  }

  def addNewDatabaseOnSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      val errorFunction: Form[DatabaseForm] => Future[Result] = { (formWithErrors: Form[DatabaseForm]) =>
        Future.successful(BadRequest(addDatabaseView(formWithErrors, newDatabaseOnSubmitLink)))
      }

      val successFunction: DatabaseForm => Future[Result] = { (dataForm: DatabaseForm) =>
        for {
          _ <- journeyCacheRepository.upsert(NewDatabase, dataForm)
        } yield Redirect(controllers.gedcom.routes.ImportGedcomController.checkYourAnswersImportGedcom)
      }

      val formValidationResult = DatabaseForm.databaseForm.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }

  def checkYourAnswersImportGedcom: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      journeyCacheRepository.get.map {
        case None        => Redirect(controllers.gedcom.routes.ImportGedcomController.showGedcomList)
        case Some(cache) =>
          Ok(checkYourAnswersView(cache.validated))
      }
  }

  def checkYourAnswersImportGedcomOnSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      Future.successful(Ok("Import process would start now..."))
  }
}
