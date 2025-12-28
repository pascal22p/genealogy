package controllers.gedcom

import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthJourney
import models.forms.extensions.FillFormExtension.filledWith
import models.forms.TrueOrFalseForm
import models.journeyCache.UserAnswersKey.*
import models.AuthenticatedRequest
import play.api.data.Form
import play.api.i18n.*
import play.api.mvc.*
import repositories.JourneyCacheRepository
import views.html.gedcom.*

@Singleton
class ClearDatabaseQuestionController @Inject() (
    authJourney: AuthJourney,
    journeyCacheRepository: JourneyCacheRepository,
    clearDatabaseContentQuestionView: ClearDatabaseContentQuestionView,
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController
    with I18nSupport {

  def showClearDatabaseQuestion: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      journeyCacheRepository.get(ClearExistingDatabaseQuestion).map { defaults =>
        val form = TrueOrFalseForm.trueOrFalseForm.filledWith(defaults)
        Ok(clearDatabaseContentQuestionView(form))
      }
  }

  def clearDatabaseQuestionOnSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      val errorFunction: Form[TrueOrFalseForm] => Future[Result] = { (formWithErrors: Form[TrueOrFalseForm]) =>
        Future.successful(BadRequest(clearDatabaseContentQuestionView(formWithErrors)))
      }

      val successFunction: TrueOrFalseForm => Future[Result] = { (dataForm: TrueOrFalseForm) =>
        journeyCacheRepository
          .upsert(ClearExistingDatabaseQuestion, dataForm)
          .map { _ =>
            Redirect(controllers.gedcom.routes.CheckYourAnswersController.checkYourAnswersImportGedcom)
          }
      }

      val formValidationResult = TrueOrFalseForm.trueOrFalseForm.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }
}
