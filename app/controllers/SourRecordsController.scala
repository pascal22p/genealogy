package controllers

import javax.inject.*

import scala.concurrent.ExecutionContext

import actions.AuthAction
import models.AuthenticatedRequest
import play.api.i18n.*
import play.api.mvc.*
import queries.GetSqlQueries
import views.html.SourCitationsFromRecordListView
import views.html.SourRecordsListView

@Singleton
class SourRecordsController @Inject() (
    authAction: AuthAction,
    getSqlQueries: GetSqlQueries,
    sourRecordsListView: SourRecordsListView,
    sourCitationsFromRecordListView: SourCitationsFromRecordListView,
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController
    with I18nSupport {

  def index(dbId: Int): Action[AnyContent] = authAction.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      getSqlQueries.getSourRecords(dbId).map { records =>
        Ok(sourRecordsListView(dbId, records))
      }
  }

  def showSourCitationsFromRecord(dbId: Int, sourRecordId: Int): Action[AnyContent] = authAction.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      for {
        sourRecordOption <- getSqlQueries.getSourRecord(sourRecordId).value
        sourCitations    <- getSqlQueries.getSourCitationsFromRecord(sourRecordId)
      } yield {
        sourRecordOption.fold(NotFound("Source record not found")) { sourRecord =>
          Ok(sourCitationsFromRecordListView(dbId, sourRecord, sourCitations))
        }
      }
  }

}
