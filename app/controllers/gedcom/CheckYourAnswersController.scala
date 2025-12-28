package controllers.gedcom

import java.nio.file.Paths
import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

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
import services.gedcom.GedcomImportService
import utils.FileUtils
import views.html.gedcom.*

@Singleton
class CheckYourAnswersController @Inject() (
    authJourney: AuthJourney,
    journeyCacheRepository: JourneyCacheRepository,
    appConfig: AppConfig,
    gedcomImportService: GedcomImportService,
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
              userAnswers => Ok(checkYourAnswersView(userAnswers.flattenByKey(ImportGedcom)))
            )
      }
  }

  def checkYourAnswersImportGedcomOnSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      journeyCacheRepository.get.flatMap {
        case None              => Future.successful(Redirect(controllers.gedcom.routes.ImportGedcomController.showGedcomList))
        case Some(userAnswers) =>
          val basePath = Paths.get(appConfig.uploadPath)
          val sanitise =
            s"./${basePath.resolve(userAnswers.getItem(ChooseGedcomFileQuestion).selectedFile).normalize()}"
          val gedcomTxt = FileUtils.ReadGedcomAsString(sanitise)

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

          val gg: Future[Result] = for {
            dbId <- maybeCreateDatabase.value
            _    <- maybeClearDatabase
            targetDbId = dbId.getOrElse(userAnswers.getItem(SelectExistingDatabaseQuestion).id)
            _ <- gedcomImportService.gedcom2sql(gedcomTxt, targetDbId)
            _ <- journeyCacheRepository.clear
          } yield {
            Redirect(controllers.routes.HomeController.onload())
          }
          gg
      }
  }
}
