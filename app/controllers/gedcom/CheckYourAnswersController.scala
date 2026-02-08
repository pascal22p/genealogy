package controllers.gedcom

import java.nio.file.Paths
import java.util.UUID
import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import actions.AuthJourney
import cats.data.OptionT
import config.AppConfig
import models.journeyCache.JourneyId.ImportGedcom
import models.journeyCache.UserAnswersKey.*
import models.AuthenticatedRequest
import models.GenealogyDatabase
import play.api.i18n.*
import play.api.mvc.*
import queries.InsertSqlQueries
import queries.UpdateSqlQueries
import repositories.JourneyCacheRepository
import services.gedcom.GedcomHashIdTable
import services.gedcom.GedcomImportService
import views.html.gedcom.*

@Singleton
class CheckYourAnswersController @Inject() (
    authJourney: AuthJourney,
    journeyCacheRepository: JourneyCacheRepository,
    appConfig: AppConfig,
    gedcomImportService: GedcomImportService,
    gedcomHashIdTable: GedcomHashIdTable,
    insertSqlQueries: InsertSqlQueries,
    updateSqlQueries: UpdateSqlQueries,
    checkYourAnswersView: CheckYourAnswersView,
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController
    with I18nSupport {

  def checkYourAnswersImportGedcom: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      journeyCacheRepository.get.map {
        case None        => Redirect(controllers.gedcom.routes.ImportGedcomController.showGedcomList)
        case Some(cache) =>
          cache
            .validated(ImportGedcom)
            .fold(
              call => Redirect(call),
              userAnswers =>
                Ok(
                  checkYourAnswersView(
                    userAnswers.flattenByKey(ImportGedcom),
                    ImportGedcom,
                    controllers.gedcom.routes.CheckYourAnswersController.checkYourAnswersImportGedcomOnSubmit
                  )
                )
            )
      }
  }

  def checkYourAnswersImportGedcomOnSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      Future.successful(Redirect(controllers.gedcom.routes.GedcomWarningsController.showWarnings))
  }

  def doGedcomImport: Action[AnyContent] = authJourney.authWithAdminRight
    .async { implicit request: AuthenticatedRequest[AnyContent] =>
      journeyCacheRepository.get.flatMap {
        case None              => Future.successful(Redirect(controllers.gedcom.routes.ImportGedcomController.showGedcomList))
        case Some(userAnswers) =>
          val basePath = Paths.get(appConfig.uploadPath)
          val sanitise =
            s"./${basePath.resolve(userAnswers.getItem(ChooseGedcomFileQuestion).selectedFile).normalize()}"

          def maybeCreateDatabase: OptionT[Future, Int] =
            if (userAnswers.getItem(CreateNewDatabaseQuestion).trueOrFalse) {
              insertSqlQueries
                .insertDatabase(
                  GenealogyDatabase(
                    0,
                    userAnswers.getItem(NewDatabaseDetailsQuestion).name,
                    userAnswers.getItem(NewDatabaseDetailsQuestion).description,
                    None
                  )
                )
            } else {
              OptionT.fromOption(None)
            }

          def maybeClearDatabase: Future[Int] =
            if (
              !userAnswers.getItem(CreateNewDatabaseQuestion).trueOrFalse &&
              userAnswers.getItem(ClearExistingDatabaseQuestion).trueOrFalse
            ) {
              updateSqlQueries
                .emptyDatabase(
                  userAnswers.getItem(SelectExistingDatabaseQuestion).id
                )
            } else {
              Future.successful(0)
            }

          val jobId = UUID.randomUUID().toString
          gedcomHashIdTable.updateJobStatus(jobId: String, "Start")
          (for {
            dbId <- maybeCreateDatabase.value
            _    <- maybeClearDatabase
            targetDbId = dbId.getOrElse(userAnswers.getItem(SelectExistingDatabaseQuestion).id)
            _ <- gedcomImportService.insertGedcomInDatabase(sanitise, targetDbId, jobId)
            _ <- journeyCacheRepository.clear
          } yield {
            gedcomHashIdTable.updateJobStatus(jobId, s"Import done")
            Redirect(controllers.gedcom.routes.ImportGedcomController.showStatus(jobId))
          }).recover {
            case NonFatal(error) =>
              gedcomHashIdTable.updateJobStatus(jobId, error.getMessage)
              throw error
          }
          Future.successful(Redirect(controllers.gedcom.routes.ImportGedcomController.showStatus(jobId)))
      }
    }
}
