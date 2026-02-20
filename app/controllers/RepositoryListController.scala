package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import actions.AuthAction
import models.AuthenticatedRequest
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import queries.GetSqlQueries
import views.html.RepositoryListView

@Singleton
class RepositoryListController @Inject() (
    authAction: AuthAction,
    getSqlQueries: GetSqlQueries,
    repositoryListView: RepositoryListView,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def showRepositories(dbId: Int): Action[AnyContent] = authAction.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      getSqlQueries.getRepositories(dbId).map { repos =>
        Ok(repositoryListView(dbId, repos.sortBy(_.name)))
      }
  }
}
