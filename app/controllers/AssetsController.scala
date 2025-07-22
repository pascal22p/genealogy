package controllers

import java.io.File
import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import config.AppConfig
import controllers.Assets
import play.api.i18n.I18nSupport
import play.api.mvc.*

@Singleton
class AssetsController @Inject() (
    assets: Assets,
    appConfig: AppConfig,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def get(filename: String): Action[AnyContent] = {
    Try(new File(filename)) match {
      case Success(file) =>
        val path = new File(s"${appConfig.externalAssetsPath}/$file")
        if (path.exists()) {
          Action(Ok.sendFile(path))
        } else {
          assets.at("/public", file.getPath)
        }
      case Failure(exception) =>
        Action(InternalServerError(s"An error occurred while looking for a filename: ${exception.getMessage}"))
    }
  }
}
