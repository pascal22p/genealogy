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
import models.forms.DatabaseForm
import models.forms.GedcomListForm
import models.forms.TrueOrFalseForm
import models.journeyCache.*
import models.journeyCache.JourneyValidation.validate
import repositories.JourneyCacheRepository
import views.html.add.AddDatabase
import views.html.gedcom.CheckYourAnswersView
import views.html.gedcom.GedcomListView
import views.html.gedcom.NewDatabaseQuestionView

@Singleton
class ImportGedcomController @Inject() (
    authJourney: AuthJourney,
    journeyCacheRepository: JourneyCacheRepository,
    appConfig: AppConfig,
    gedcomListView: GedcomListView,
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
      val form = GedcomListForm.form
      Future.successful(Ok(gedcomListView(form, listGedcomFiles)))
  }

  def gedcomListOnSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      val errorFunction: Form[GedcomListForm] => Future[Result] = { (formWithErrors: Form[GedcomListForm]) =>
        Future.successful(BadRequest(gedcomListView(formWithErrors, listGedcomFiles)))
      }

      val successFunction: GedcomListForm => Future[Result] = { (dataForm: GedcomListForm) =>
        val basePath = Paths.get(appConfig.uploadPath)
        val sanitise = s"./${basePath.resolve(dataForm.selectedFile).normalize()}"
        if (sanitise.startsWith(s"$basePath") && Files.exists(Paths.get(sanitise))) {
          // val gedcomTxt = readFile.asString(sanitise)
          // val nodes     = gedcomCommonParser.getTree(gedcomTxt)
          // val sqls      = gedcomImportService.convertTree2SQL(nodes, 4)
          journeyCacheRepository.upsert(request.localSession.sessionId, GedcomPath, sanitise).map { _ =>
            Redirect(controllers.gedcom.routes.ImportGedcomController.showNewDatabaseQuestion)
          }
        } else {
          throw new IllegalArgumentException("Invalid file path")
        }
      }

      val formValidationResult = GedcomListForm.form.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }

  def showNewDatabaseQuestion: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      val form = TrueOrFalseForm.trueOrFalseForm
      Future.successful(Ok(newDatabaseQuestionView(form)))
  }

  def newDatabaseQuestionOnSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      val errorFunction: Form[TrueOrFalseForm] => Future[Result] = { (formWithErrors: Form[TrueOrFalseForm]) =>
        Future.successful(BadRequest(newDatabaseQuestionView(formWithErrors)))
      }

      val successFunction: TrueOrFalseForm => Future[Result] = { (dataForm: TrueOrFalseForm) =>
        journeyCacheRepository
          .upsert(request.localSession.sessionId, NewDatabaseQuestion, s"${dataForm.trueOrFalse}")
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
      val form = DatabaseForm.databaseForm
      Future.successful(Ok(addDatabaseView(form, newDatabaseOnSubmitLink)))
  }

  def addNewDatabaseOnSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      val errorFunction: Form[DatabaseForm] => Future[Result] = { (formWithErrors: Form[DatabaseForm]) =>
        Future.successful(BadRequest(addDatabaseView(formWithErrors, newDatabaseOnSubmitLink)))
      }

      val successFunction: DatabaseForm => Future[Result] = { (dataForm: DatabaseForm) =>
        for {
          _ <- journeyCacheRepository.upsert(request.localSession.sessionId, NewDatabaseName, s"${dataForm.name}")
          _ <- journeyCacheRepository.upsert(
            request.localSession.sessionId,
            NewDatabaseDescription,
            s"${dataForm.description}"
          )
        } yield Redirect(controllers.gedcom.routes.ImportGedcomController.checkYourAnswersImportGedcom)
      }

      val formValidationResult = DatabaseForm.databaseForm.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }

  def checkYourAnswersImportGedcom: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      journeyCacheRepository.get(request.localSession.sessionId).map {
        case None        => Redirect(controllers.gedcom.routes.ImportGedcomController.showGedcomList)
        case Some(cache) =>
          val validatedAnswers = cache.data.filterNot((key, _) => cache.data.validate.contains(key))

          Ok(checkYourAnswersView(validatedAnswers))
      }
  }

  def checkYourAnswersImportGedcomOnSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      Future.successful(Ok("Import process would start now..."))
  }
}
