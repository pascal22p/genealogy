package controllers

import javax.inject.*

import scala.concurrent.ExecutionContext

import actions.AuthAction
import models.AuthenticatedRequest
import cats.data.OptionT
import play.api.i18n.*
import play.api.mvc.*
import services.GenealogyDatabaseService
import queries.GetSqlQueries
import views.html.SourCitationsFromRecordListView
import views.html.SourRecordsListView

@Singleton
class SourRecordsController @Inject() (
    authAction: AuthAction,
    getSqlQueries: GetSqlQueries,
    sourRecordsListView: SourRecordsListView,
    sourCitationsFromRecordListView: SourCitationsFromRecordListView,
    genealogyDatabaseService: GenealogyDatabaseService,
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController
    with I18nSupport {

  def index(dbId: Int): Action[AnyContent] = authAction.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      (for {
        database <- OptionT(genealogyDatabaseService.getGenealogyDatabase(dbId))
        records  <- OptionT.liftF(getSqlQueries.getSourRecords(dbId))
      } yield {
        Ok(sourRecordsListView(Some(database), records))
      }).getOrElse(NotFound(s"Genealogy database $dbId not found"))
  }

  def showSourCitationsFromRecord(dbId: Int, sourRecordId: Int): Action[AnyContent] = authAction.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      (for {
        database      <- OptionT(genealogyDatabaseService.getGenealogyDatabase(dbId))
        sourRecord    <- getSqlQueries.getSourRecord(sourRecordId)
        sourCitations <- OptionT.liftF(getSqlQueries.getSourCitationsFromRecord(sourRecordId))
      } yield {
        Ok(sourCitationsFromRecordListView(Some(database), sourRecord, sourCitations))
      }).getOrElse(NotFound("database or source record not found"))
  }

}
