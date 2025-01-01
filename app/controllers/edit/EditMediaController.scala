package controllers.edit

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthAction
import actions.AuthJourney
import cats.implicits.*
import models.forms.SourCitationForm
import models.SourCitation
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import play.api.mvc.Result
import play.api.Logging
import queries.GetSqlQueries
import queries.UpdateSqlQueries
import services.EventService
import services.PersonService
import services.SessionService
import views.html.edit.AddMedia
import views.html.ServiceUnavailable
import services.SourCitationService
import models.SourCitationType.IndividualSourCitation
import models.SourCitationType
import models.SourCitationType.UnknownSourCitation
import org.apache.pekko.http.scaladsl.model.HttpCharsetRange.*
import models.AuthenticatedRequest
import play.api.mvc.AnyContent
import models.SourCitationType.EventSourCitation
import models.SourCitationType.FamilySourCitation
import play.api.libs.Files.TemporaryFile
import play.api.libs.Files
import play.api.mvc.MultipartFormData
import play.api.mvc.Action
import java.nio.file.Paths

@Singleton
class EditMediaController @Inject() (
    authJourney: AuthJourney,
    eventService: EventService,
    personService: PersonService,
    sourCitationService: SourCitationService,
    sessionService: SessionService,
    getSqlQueries: GetSqlQueries,
    updateSqlQueries: UpdateSqlQueries,
    addMediaView: AddMedia,
    serviceUnavailableView: ServiceUnavailable,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport
    with Logging {

  def showForm() = authJourney.authWithAdminRight.async { implicit request =>
    Future.successful(Ok(addMediaView()))
  }

  def upload: Action[MultipartFormData[Files.TemporaryFile]] = authJourney.authWithAdminRight.async(parse.multipartFormData) { implicit request =>
  request.body
    .file("picture")
    .map { picture =>
      // only get the last part of the filename
      // otherwise someone can send a path like ../../home/foo/bar.txt to write to other files on the system
      val filename    = Paths.get(picture.filename).getFileName
      val fileSize    = picture.fileSize
      val contentType = picture.contentType

      picture.ref.copyTo(Paths.get(s"public/$filename"), replace = false)
      Future.successful(Ok("File uploaded"))
    }
    .getOrElse {
      Future.successful(InternalServerError(serviceUnavailableView("Missing file")))
    }
}

  def onSubmit(id: Int) = authJourney.authWithAdminRight.async { implicit request =>
    def errorFunction(formWithErrors: Form[SourCitationForm]): Future[Result] = {
        Future.successful(BadRequest(addMediaView()))
    }

    val successFunction: SourCitationForm => Future[Result] = { dataForm =>
        Future.successful(InternalServerError(serviceUnavailableView("No record was updated")))
    }

    val formValidationResult = SourCitationForm.sourCitationForm.bindFromRequest()
    formValidationResult.fold(errorFunction, successFunction)
  }

}



