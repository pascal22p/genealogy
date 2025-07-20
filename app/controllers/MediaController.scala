package controllers
import java.io.File
import javax.inject._

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import config.AppConfig
import play.api.i18n.I18nSupport
import play.api.mvc._
import queries.GetSqlQueries

@Singleton
class MediaController @Inject() (
    appConfig: AppConfig,
    getSqlQueries: GetSqlQueries,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def serveMedia(dbId: Int, filename: String): Action[AnyContent] = Action.async {
    getSqlQueries.getGenealogyDatabase(dbId).fold(NotFound("Database not found")) { database =>
      Try(new File(filename)) match {
        case Success(file) =>
          val path = new File(s"${appConfig.mediaPath}${database.medias.getOrElse(database.name)}/$file")
          if (path.exists()) {
            Ok.sendFile(path)
          } else {
            NotFound(s"File `${path.getName}` not found")
          }
        case Failure(exception) =>
          InternalServerError(s"An error occurred while looking for a filename: ${exception.getMessage}")
      }
    }
  }
}
