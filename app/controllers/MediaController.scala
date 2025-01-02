package controllers
import javax.inject._
import play.api.mvc._
import play.api.i18n.I18nSupport
import scala.concurrent.ExecutionContext
import java.io.File
import config.AppConfig
import scala.util.{Try, Success, Failure}
import java.nio.file.NoSuchFileException

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
