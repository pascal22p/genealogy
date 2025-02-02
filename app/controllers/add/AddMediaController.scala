package controllers.add

import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthJourney
import play.api.i18n.I18nSupport
import play.api.libs.Files
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import play.api.mvc.MultipartFormData
import play.api.Logging
import queries.GetSqlQueries
import views.html.add.AddMedia
import views.html.ServiceUnavailable

@Singleton
class AddMediaController @Inject() (
    authJourney: AuthJourney,
    getSqlQueries: GetSqlQueries,
    addMediaView: AddMedia,
    serviceUnavailableView: ServiceUnavailable,
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController
    with I18nSupport
    with Logging {

  def showForm(baseId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async { implicit request =>
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

          getSqlQueries
            .getGenealogyDatabase(baseId)
            .fold(NotFound("Genealogy database not found")) { genealogyDb =>
              picture.ref.copyTo(Paths.get(s"Medias/${genealogyDb.name}/$filename"), replace = false)
              Ok(s"File uploaded Medias/${genealogyDb.name}/$filename")
            }
        }
        .getOrElse {
          Future.successful(InternalServerError(serviceUnavailableView("Missing file")))
        }
    }
}
