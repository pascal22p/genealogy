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
import models.forms.CreateNewDatabaseForm
import models.forms.GedcomPathInputTextForm
import models.forms.SelectExistingDatabaseForm
import models.forms.TrueOrFalseForm
import models.journeyCache.UserAnswersKey.*
import repositories.JourneyCacheRepository
import services.GenealogyDatabaseService
import views.html.add.AddDatabase
import views.html.gedcom.GedcomChooseDatabaseView
import views.html.gedcom.GedcomListView
import views.html.gedcom.NewDatabaseQuestionView

@Singleton
class ImportGedcomController @Inject() (
    authJourney: AuthJourney,
    journeyCacheRepository: JourneyCacheRepository,
    appConfig: AppConfig,
    genealogyDatabaseService: GenealogyDatabaseService,
    gedcomListView: GedcomListView,
    newDatabaseQuestionView: NewDatabaseQuestionView,
    addDatabaseView: AddDatabase,
    gedcomChooseDatabaseView: GedcomChooseDatabaseView,
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
      journeyCacheRepository.get(ChooseGedcomFileQuestion).map { defaults =>
        val form = GedcomPathInputTextForm.form(appConfig.uploadPath).filledWith(defaults)
        Ok(gedcomListView(form, listGedcomFiles))
      }
  }

  def gedcomListOnSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      val errorFunction: Form[GedcomPathInputTextForm] => Future[Result] = {
        (formWithErrors: Form[GedcomPathInputTextForm]) =>
          Future.successful(BadRequest(gedcomListView(formWithErrors, listGedcomFiles)))
      }

      val successFunction: GedcomPathInputTextForm => Future[Result] = { (dataForm: GedcomPathInputTextForm) =>
        journeyCacheRepository.upsert(ChooseGedcomFileQuestion, dataForm).map { _ =>
          Redirect(controllers.gedcom.routes.GedcomStatsController.gedcomStats)
        }
      }

      val formValidationResult = GedcomPathInputTextForm.form(appConfig.uploadPath).bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }

  def showNewDatabaseQuestion: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      journeyCacheRepository.get(CreateNewDatabaseQuestion).map { defaults =>
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
          .upsert(CreateNewDatabaseQuestion, dataForm)
          .map { _ =>
            if (dataForm.trueOrFalse) {
              Redirect(controllers.gedcom.routes.ImportGedcomController.addNewDatabase)
            } else {
              Redirect(controllers.gedcom.routes.ImportGedcomController.gedcomDatabaseSelect)
            }
          }
      }

      val formValidationResult = TrueOrFalseForm.trueOrFalseForm.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }

  def addNewDatabase: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      journeyCacheRepository.get(NewDatabaseDetailsQuestion).map { defaults =>
        val form = CreateNewDatabaseForm.databaseForm.filledWith(defaults)
        Ok(addDatabaseView(form, newDatabaseOnSubmitLink))
      }
  }

  def addNewDatabaseOnSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      val errorFunction: Form[CreateNewDatabaseForm] => Future[Result] = {
        (formWithErrors: Form[CreateNewDatabaseForm]) =>
          Future.successful(BadRequest(addDatabaseView(formWithErrors, newDatabaseOnSubmitLink)))
      }

      val successFunction: CreateNewDatabaseForm => Future[Result] = { (dataForm: CreateNewDatabaseForm) =>
        for {
          _ <- journeyCacheRepository.upsert(NewDatabaseDetailsQuestion, dataForm)
        } yield Redirect(controllers.gedcom.routes.CheckYourAnswersController.checkYourAnswersImportGedcom)
      }

      val formValidationResult = CreateNewDatabaseForm.databaseForm.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }

  def gedcomDatabaseSelect: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      journeyCacheRepository.get(SelectExistingDatabaseQuestion).flatMap { defaults =>
        genealogyDatabaseService.getGenealogyDatabases.map { dbs =>
          val form = SelectExistingDatabaseForm.form.filledWith(defaults)
          Ok(gedcomChooseDatabaseView(form, dbs))
        }
      }
  }

  def gedcomDatabaseSelectOnSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      val errorFunction: Form[SelectExistingDatabaseForm] => Future[Result] = {
        (formWithErrors: Form[SelectExistingDatabaseForm]) =>
          genealogyDatabaseService.getGenealogyDatabases.map { dbs =>
            BadRequest(gedcomChooseDatabaseView(formWithErrors, dbs))
          }
      }

      val successFunction: SelectExistingDatabaseForm => Future[Result] = { (dataForm: SelectExistingDatabaseForm) =>
        for {
          _ <- journeyCacheRepository.upsert(SelectExistingDatabaseQuestion, dataForm)
        } yield Redirect(controllers.gedcom.routes.ClearDatabaseQuestionController.showClearDatabaseQuestion)
      }

      val formValidationResult = SelectExistingDatabaseForm.form.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }
}
