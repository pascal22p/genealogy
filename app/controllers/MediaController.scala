package controllers
import java.io.File
import java.nio.file.NoSuchFileException
import javax.inject._

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import config.AppConfig
import play.api.i18n.I18nSupport
import play.api.mvc._

@Singleton
class MediaController @Inject() (
    appConfig: AppConfig,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def serveMedia(filename: String) = Action {
    Try(new File(filename)) match {
      case Success(file) =>
        val path = new File(s"${appConfig.mediaPath}Parois/$file")
        if (path.exists()) {
          Ok.sendFile(path)
        } else {
          NotFound(s"File `${path.getName()}` not found")
        }
      case Failure(exception) =>
        InternalServerError(s"An error occurred while looking for a filename: ${exception.getMessage}")
    }
  }
}
