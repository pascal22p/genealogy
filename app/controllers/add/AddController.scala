package controllers.add

import javax.inject.*

import scala.concurrent.ExecutionContext

import actions.AuthJourney
import models.AuthenticatedRequest
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
      genealogyDatabaseService.getGenealogyDatabases.map { dbs =>
        dbs.find(_.id == dbId).fold(NotFound(s"db id $dbId cannot be found")) { db =>
          Ok(addView(db))
        }
      }
  }

}
