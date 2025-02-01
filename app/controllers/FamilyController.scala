package controllers

import javax.inject.*

import scala.concurrent.Future

import actions.AuthAction
import models.AuthenticatedRequest
import play.api.i18n.*
import play.api.mvc.*

@Singleton
class FamilyController @Inject() (
    authAction: AuthAction,
    val controllerComponents: ControllerComponents
)(
) extends BaseController
    with I18nSupport {

  def showFamily(baseId: Int, id: Int): Action[AnyContent] = authAction.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      Future.successful(NotImplemented(s"$baseId $id"))
  }
}
