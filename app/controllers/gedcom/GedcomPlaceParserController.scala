package controllers.gedcom

import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthJourney
import config.AppConfig
import models.journeyCache.UserAnswersKey.*
import models.journeyCache.UserAnswersKey.PlacesElementsQuestion
import models.AuthenticatedRequest
import play.api.data.Form
import play.api.i18n.*
import play.api.mvc.*
import repositories.JourneyCacheRepository
import services.gedcom.GedcomCommonParser
import utils.Constants
import views.html.gedcom.GedcomPlacesSampleView
import models.forms.extensions.FillFormExtension.filledWith
import models.forms.PlacesElementsForm

@Singleton
class GedcomPlaceParserController @Inject() (
    authJourney: AuthJourney,
    journeyCacheRepository: JourneyCacheRepository,
    appConfig: AppConfig,
    gedcomCommonParser: GedcomCommonParser,
    gedcomPlacesSampleView: GedcomPlacesSampleView,
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController
    with I18nSupport {

  def chooseExtractPlaceElements: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      for {
        maybeChooseGedcomFileQuestion <- journeyCacheRepository.get(ChooseGedcomFileQuestion)
        maybePlacesElementsQuestion   <- journeyCacheRepository.get(PlacesElementsQuestion)
      } yield {
        (maybeChooseGedcomFileQuestion, maybePlacesElementsQuestion) match {
          case (Some(chooseGedcomFileQuestion), placesElementsQuestion) =>
            val basePath = Paths.get(appConfig.uploadPath)
            val sanitise = s"./${basePath.resolve(chooseGedcomFileQuestion.selectedFile).normalize()}"
            if (sanitise.startsWith(s"$basePath") && Files.exists(Paths.get(sanitise))) {
              val placesSample = gedcomCommonParser.getSamplePlaces(sanitise, Constants.maxSampleSize * 10)
              val form         = PlacesElementsForm.form.filledWith(placesElementsQuestion)

              Ok(gedcomPlacesSampleView(placesSample, form))
            } else {
              InternalServerError("Gedcom file path should be valid")
            }
          case _ => Redirect(controllers.gedcom.routes.ImportGedcomController.startJourney)
        }
      }
  }

  def chooseExtractPlaceElementsOnSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      val errorFunction: Form[PlacesElementsForm] => Future[Result] = { (formWithErrors: Form[PlacesElementsForm]) =>
        journeyCacheRepository.get(ChooseGedcomFileQuestion).map {
          case Some(chooseGedcomFileQuestion) =>
            val basePath = Paths.get(appConfig.uploadPath)
            val sanitise = s"./${basePath.resolve(chooseGedcomFileQuestion.selectedFile).normalize()}"
            if (sanitise.startsWith(s"$basePath") && Files.exists(Paths.get(sanitise))) {
              val placesSample = gedcomCommonParser.getSamplePlaces(sanitise, Constants.maxSampleSize * 10)
              BadRequest(gedcomPlacesSampleView(placesSample, formWithErrors))
            } else {
              InternalServerError("Gedcom file path should be valid")
            }
          case _ => Redirect(controllers.gedcom.routes.ImportGedcomController.startJourney)
        }
      }

      val successFunction: PlacesElementsForm => Future[Result] = { (dataForm: PlacesElementsForm) =>
        Future.successful(Ok(s"${dataForm.hierarchy}"))

        /*journeyCacheRepository
          .upsert(PlacesElementsQuestion, dataForm)
          .map { _ =>
            Redirect(controllers.gedcom.routes.CheckYourAnswersController.checkYourAnswersImportGedcom)
          }*/
      }

      val formValidationResult = PlacesElementsForm.form.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }
}
