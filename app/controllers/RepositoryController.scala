package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import actions.AuthAction
import models.AuthenticatedRequest
import cats.data.OptionT
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import services.GenealogyDatabaseService
import queries.GetSqlQueries
import views.html.RepositoryListView
import views.html.RepositoryView

@Singleton
class RepositoryController @Inject() (
    authAction: AuthAction,
    getSqlQueries: GetSqlQueries,
    repositoryListView: RepositoryListView,
    repositoryView: RepositoryView,
    genealogyDatabaseService: GenealogyDatabaseService,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def showRepositories(dbId: Int): Action[AnyContent] = authAction.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      (for {
        database <- OptionT(genealogyDatabaseService.getGenealogyDatabase(dbId))
        repos    <- OptionT.liftF(getSqlQueries.getRepositories(dbId))
      } yield {
        Ok(repositoryListView(Some(database), repos.sortBy(_.name)))
      }).getOrElse(NotFound(s"Genealogy database $dbId not found"))
  }

  def showRepository(dbId: Int, repoId: Int): Action[AnyContent] = authAction.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      (for {
        database <- OptionT(genealogyDatabaseService.getGenealogyDatabase(dbId))
        repo     <- getSqlQueries.getRepository(dbId, repoId)
      } yield {
        Ok(repositoryView(Some(database), repo))
      }).getOrElse(NotFound(s"Genealogy database $dbId not found"))
  }
}
