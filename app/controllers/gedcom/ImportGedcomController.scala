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
import models.GenealogyDatabase
import play.api.data.Form
import play.api.i18n.*
import play.api.mvc.*
import play.api.mvc.AnyContent
import config.AppConfig
import models.forms.extensions.FillFormExtension.filledWith
import models.forms.GedcomDatabaseForm
import models.forms.GedcomListForm
import models.forms.NewDatabaseForm
import models.forms.TrueOrFalseForm
import models.journeyCache.UserAnswersKey.*
import queries.InsertSqlQueries
import repositories.JourneyCacheRepository
import services.gedcom.GedcomCommonParser
import services.gedcom.GedcomImportService
import services.GenealogyDatabaseService
import utils.ReadFile
import views.html.add.AddDatabase
import views.html.gedcom.CheckYourAnswersView
import views.html.gedcom.GedcomChooseDatabaseView
import views.html.gedcom.GedcomListView
import views.html.gedcom.GedcomStatsView
import views.html.gedcom.NewDatabaseQuestionView
import models.journeyCache.UserAnswersExtensions.getItem

@Singleton
class ImportGedcomController @Inject() (
    authJourney: AuthJourney,
    journeyCacheRepository: JourneyCacheRepository,
    appConfig: AppConfig,
    readFile: ReadFile,
    genealogyDatabaseService: GenealogyDatabaseService,
    gedcomCommonParser: GedcomCommonParser,
    gedcomImportService: GedcomImportService,
    insertSqlQueries: InsertSqlQueries,
    gedcomListView: GedcomListView,
    gedcomStatsView: GedcomStatsView,
    newDatabaseQuestionView: NewDatabaseQuestionView,
    addDatabaseView: AddDatabase,
    gedcomChooseDatabaseView: GedcomChooseDatabaseView,
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
            val gedcomTxt    = readFile.asString(sanitise)
            val gedcomObject = gedcomCommonParser.getTree(gedcomTxt)
            val sqlsIor      = gedcomImportService.convertTree2SQL(gedcomObject.nodes, 0)

            val warnings = sqlsIor.left.getOrElse(List.empty)
            val sqls     = sqlsIor.right.getOrElse(List.empty).zipWithIndex.map { (value, idx) =>
              s"${idx + 1}. ${value.sql} === ${value.params}"
            }
            Ok(gedcomStatsView(warnings, sqls, gedcomObject))
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
              Redirect(controllers.gedcom.routes.ImportGedcomController.gedcomDatabaseSelect)
            }
          }
      }

      val formValidationResult = TrueOrFalseForm.trueOrFalseForm.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }

  def addNewDatabase: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      journeyCacheRepository.get(NewDatabase).map { defaults =>
        val form = NewDatabaseForm.databaseForm.filledWith(defaults)
        Ok(addDatabaseView(form, newDatabaseOnSubmitLink))
      }
  }

  def addNewDatabaseOnSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      val errorFunction: Form[NewDatabaseForm] => Future[Result] = { (formWithErrors: Form[NewDatabaseForm]) =>
        Future.successful(BadRequest(addDatabaseView(formWithErrors, newDatabaseOnSubmitLink)))
      }

      val successFunction: NewDatabaseForm => Future[Result] = { (dataForm: NewDatabaseForm) =>
        for {
          _ <- journeyCacheRepository.upsert(NewDatabase, dataForm)
        } yield Redirect(controllers.gedcom.routes.ImportGedcomController.checkYourAnswersImportGedcom)
      }

      val formValidationResult = NewDatabaseForm.databaseForm.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }

  def gedcomDatabaseSelect: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      journeyCacheRepository.get(DatabaseSelect).flatMap { defaults =>
        genealogyDatabaseService.getGenealogyDatabases.map { dbs =>
          val form = GedcomDatabaseForm.form.filledWith(defaults)
          Ok(gedcomChooseDatabaseView(form, dbs))
        }
      }
  }

  def gedcomDatabaseSelectOnSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      val errorFunction: Form[GedcomDatabaseForm] => Future[Result] = { (formWithErrors: Form[GedcomDatabaseForm]) =>
        genealogyDatabaseService.getGenealogyDatabases.map { dbs =>
          BadRequest(gedcomChooseDatabaseView(formWithErrors, dbs))
        }
      }

      val successFunction: GedcomDatabaseForm => Future[Result] = { (dataForm: GedcomDatabaseForm) =>
        for {
          _ <- journeyCacheRepository.upsert(DatabaseSelect, dataForm)
        } yield Redirect(controllers.gedcom.routes.ImportGedcomController.checkYourAnswersImportGedcom)
      }

      val formValidationResult = GedcomDatabaseForm.form.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }

  def checkYourAnswersImportGedcom: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      journeyCacheRepository.get.map {
        case None        => Redirect(controllers.gedcom.routes.ImportGedcomController.showGedcomList)
        case Some(cache) =>
          Ok(checkYourAnswersView(cache.validated.flattenByKey))
      }
  }

  def checkYourAnswersImportGedcomOnSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      journeyCacheRepository.get.flatMap {
        case None              => Future.successful(Redirect(controllers.gedcom.routes.ImportGedcomController.showGedcomList))
        case Some(userAnswers) =>
          val basePath  = Paths.get(appConfig.uploadPath)
          val sanitise  = s"./${basePath.resolve(userAnswers.data.getItem(GedcomPath).selectedFile).normalize()}"
          val gedcomTxt = readFile.asString(sanitise)

          if (userAnswers.data.getItem(NewDatabaseQuestion).trueOrFalse) {
            insertSqlQueries
              .insertDatabase(
                GenealogyDatabase(
                  0,
                  userAnswers.data.getItem(NewDatabase).name,
                  userAnswers.data.getItem(NewDatabase).description,
                  None
                )
              )
              .foldF(throw new RuntimeException("Error while creating database")) { id =>
                gedcomImportService.gedcom2sql(gedcomTxt, id).flatMap { _ =>
                  journeyCacheRepository.clear.map { _ =>
                    Redirect(controllers.routes.HomeController.onload())
                  }
                }
              }
          } else {
            gedcomImportService
              .gedcom2sql(
                gedcomTxt,
                userAnswers.data.getItem(DatabaseSelect).id
              )
              .flatMap { _ =>
                journeyCacheRepository.clear.map { _ =>
                  Redirect(controllers.routes.HomeController.onload())
                }
              }
          }
      }
  }
}
