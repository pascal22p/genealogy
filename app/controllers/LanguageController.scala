package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.Future

import actions.AuthAction
import play.api.i18n.I18nSupport
import play.api.i18n.Lang
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import play.api.Logging

@Singleton
class LanguageController @Inject() (
    authAction: AuthAction,
    val controllerComponents: ControllerComponents
) extends BaseController
    with I18nSupport
    with Logging {

  def switchToLanguage(lang: String): Action[AnyContent] = authAction.async { implicit request =>
    val newLang = Lang.get(lang).getOrElse(Lang.defaultLang)
    Future.successful(
      Redirect(request.headers.get(REFERER).getOrElse("/"))
        .withLang(newLang)
    )
  }
}
