package controllers

import javax.inject.*

import scala.concurrent.ExecutionContext

import actions.AuthAction
import models.AuthenticatedRequest
import play.api.i18n.*
import play.api.mvc.*
import services.GenealogyDatabaseService
import views.html.ViewView

@Singleton
class ViewController @Inject() (
    authAction: AuthAction,
    genealogyDatabaseService: GenealogyDatabaseService,
    viewView: ViewView,
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController
    with I18nSupport {

  def index(dbId: Int): Action[AnyContent] = authAction.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      genealogyDatabaseService.getGenealogyDatabases.map { dbs =>
        dbs.find(_.id == dbId).fold(NotFound(s"db id $dbId cannot be found")) { db =>
          Ok(viewView(db))
        }
      }
  }

}
