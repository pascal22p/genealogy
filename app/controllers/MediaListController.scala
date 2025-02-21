package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import actions.AuthJourney
import models.AuthenticatedRequest
import models.MediaType.UnknownMedia
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import queries.GetSqlQueries
import views.html.MediaList

@Singleton
class MediaListController @Inject() (
    authJourney: AuthJourney,
    getSqlQueries: GetSqlQueries,
    mediaListView: MediaList,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def showMedias(dbId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      getSqlQueries.getMedias(None, UnknownMedia, dbId).map { medias =>
        Ok(mediaListView(dbId, medias.sortBy(_.timestamp).reverse))
      }
  }
}
