package controllers.add

import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.Future

import actions.AuthJourney
import models.forms.SourCitationForm
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.Files
import play.api.mvc.Action
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import play.api.mvc.MultipartFormData
import play.api.mvc.Result
import play.api.Logging
import views.html.add.AddMedia
import views.html.ServiceUnavailable

@Singleton
class AddMediaController @Inject() (
    authJourney: AuthJourney,
    addMediaView: AddMedia,
    serviceUnavailableView: ServiceUnavailable,
    val controllerComponents: ControllerComponents
) extends BaseController
    with I18nSupport
    with Logging {

  def showForm(baseId: Int) = authJourney.authWithAdminRight.async { implicit request =>
    Future.successful(Ok(addMediaView(baseId)))
  }

  def upload(baseId: Int): Action[MultipartFormData[Files.TemporaryFile]] =
    authJourney.authWithAdminRight.async(parse.multipartFormData) { implicit request =>
      request.body
        .file("picture")
        .map { picture =>
          // only get the last part of the filename
          // otherwise someone can send a path like ../../home/foo/bar.txt to write to other files on the system
          val filename = Paths.get(picture.filename).getFileName
          // picture.fileSize
          // picture.contentType

          picture.ref.copyTo(Paths.get(s"public/$filename"), replace = false)
          Future.successful(Ok("File uploaded"))
        }
        .getOrElse {
          Future.successful(InternalServerError(serviceUnavailableView("Missing file")))
        }
    }

  def onSubmit(baseId: Int, id: Int) = authJourney.authWithAdminRight.async { implicit request =>
    def errorFunction(formWithErrors: Form[SourCitationForm]): Future[Result] = {
      Future.successful(BadRequest(addMediaView(baseId)))
    }

    val successFunction: SourCitationForm => Future[Result] = { dataForm =>
      Future.successful(InternalServerError(serviceUnavailableView("No record was updated")))
    }

    val formValidationResult = SourCitationForm.sourCitationForm.bindFromRequest()
    formValidationResult.fold(errorFunction, successFunction)
  }

}
