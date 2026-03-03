package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import actions.AuthJourney
import models.AuthenticatedRequest
import cats.data.OptionT
import models.MediaType.UnknownMedia
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import services.GenealogyDatabaseService
import queries.GetSqlQueries
import views.html.MediaList

@Singleton
class MediaListController @Inject() (
    authJourney: AuthJourney,
    getSqlQueries: GetSqlQueries,
    mediaListView: MediaList,
    genealogyDatabaseService: GenealogyDatabaseService,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def showMedias(dbId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      (for {
        database <- OptionT(genealogyDatabaseService.getGenealogyDatabase(dbId))
        medias   <- OptionT.liftF(getSqlQueries.getMedias(None, UnknownMedia, dbId))
      } yield {
        Ok(mediaListView(Some(database), medias.sortBy(_.timestamp).reverse))
      }).getOrElse(NotFound(s"Genealogy database $dbId not found"))
  }
}
