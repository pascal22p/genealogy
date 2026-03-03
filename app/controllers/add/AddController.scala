package controllers.add

import javax.inject.*

import scala.concurrent.ExecutionContext

import actions.AuthJourney
import models.AuthenticatedRequest
import cats.data.OptionT
import play.api.i18n.*
import play.api.mvc.*
import services.GenealogyDatabaseService
import views.html.add.AddView

@Singleton
class AddController @Inject() (
    authJourney: AuthJourney,
    genealogyDatabaseService: GenealogyDatabaseService,
    addView: AddView,
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController
    with I18nSupport {

  def index(dbId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      (for {
        database <- OptionT(genealogyDatabaseService.getGenealogyDatabase(dbId))
      } yield {
        Ok(addView(Some(database)))
      }).getOrElse(NotFound(s"db id $dbId cannot be found"))
  }

}
