package controllers.gedcom

import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthJourney
import config.AppConfig
import models.forms.extensions.FillFormExtension.filledWith
import models.forms.PlacesElementsPaddingForm
import models.journeyCache.UserAnswersKey.*
import models.AuthenticatedRequest
import play.api.data.Form
import play.api.i18n.*
import play.api.mvc.*
import repositories.JourneyCacheRepository
import services.gedcom.GedcomCommonParser
import utils.Constants
import views.html.gedcom.ChoosePlacePaddingView

@Singleton
class ChoosePlacePaddingController @Inject() (
    authJourney: AuthJourney,
    appConfig: AppConfig,
    gedcomCommonParser: GedcomCommonParser,
    journeyCacheRepository: JourneyCacheRepository,
    choosePlacePaddingView: ChoosePlacePaddingView,
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController
    with I18nSupport {

  def showForm: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      for {
        maybeChooseGedcomFileQuestion <- journeyCacheRepository.get(ChooseGedcomFileQuestion)
        maybePlacesPaddingQuestion    <- journeyCacheRepository.get(PlacesElementsPaddingQuestion)
      } yield {
        (maybeChooseGedcomFileQuestion, maybePlacesPaddingQuestion) match {
          case (Some(chooseGedcomFileQuestion), placesElementsQuestion) =>
            val basePath = Paths.get(appConfig.uploadPath)
            val sanitise = s"./${basePath.resolve(chooseGedcomFileQuestion.selectedFile).normalize()}"
            if (sanitise.startsWith(s"$basePath") && Files.exists(Paths.get(sanitise))) {
              val placesSample = gedcomCommonParser.getSamplePlaces(sanitise, Constants.maxSampleSize * 10)
              val form         = PlacesElementsPaddingForm.form.filledWith(placesElementsQuestion)
              Ok(choosePlacePaddingView(form, placesSample))
            } else {
              InternalServerError("Gedcom file path should be valid")
            }

          case _ => Redirect(controllers.gedcom.routes.ImportGedcomController.startJourney)
        }
      }
  }

  def showFormOnSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      val errorFunction: Form[PlacesElementsPaddingForm] => Future[Result] = {
        (formWithErrors: Form[PlacesElementsPaddingForm]) =>
          journeyCacheRepository.get(ChooseGedcomFileQuestion).map {
            case Some(chooseGedcomFileQuestion) =>
              val basePath = Paths.get(appConfig.uploadPath)
              val sanitise = s"./${basePath.resolve(chooseGedcomFileQuestion.selectedFile).normalize()}"
              if (sanitise.startsWith(s"$basePath") && Files.exists(Paths.get(sanitise))) {
                val placesSample = gedcomCommonParser.getSamplePlaces(sanitise, Constants.maxSampleSize * 10)
                BadRequest(choosePlacePaddingView(formWithErrors, placesSample))
              } else {
                InternalServerError("Gedcom file path should be valid")
              }
            case _ => Redirect(controllers.gedcom.routes.ImportGedcomController.startJourney)
          }
      }

      val successFunction: PlacesElementsPaddingForm => Future[Result] = { (dataForm: PlacesElementsPaddingForm) =>
        journeyCacheRepository.upsert(PlacesElementsPaddingQuestion, dataForm).map { _ =>
          Redirect(controllers.gedcom.routes.GedcomPlaceParserController.chooseExtractPlaceElements)
        }
      }

      val formValidationResult = PlacesElementsPaddingForm.form.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }

}
