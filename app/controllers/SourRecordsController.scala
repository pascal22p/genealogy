package controllers

import javax.inject.*

import scala.concurrent.ExecutionContext

import actions.AuthAction
import models.AuthenticatedRequest
import cats.data.OptionT
import cats.implicits.toTraverseOps
import play.api.i18n.*
import play.api.mvc.*
import services.GenealogyDatabaseService
import queries.GetCitationUsagesSqlQueries
import queries.GetSqlQueries
import views.html.SourCitationsFromRecordListView
import views.html.SourRecordsListView
import viewModels.GedcomObjectUsageViewModels
import viewModels.SourCitationViewModel

@Singleton
class SourRecordsController @Inject() (
    authAction: AuthAction,
    getSqlQueries: GetSqlQueries,
    getCitationUsagesSqlQueries: GetCitationUsagesSqlQueries,
    sourRecordsListView: SourRecordsListView,
    sourCitationsFromRecordListView: SourCitationsFromRecordListView,
    genealogyDatabaseService: GenealogyDatabaseService,
    gedcomObjectUsageViewModels: GedcomObjectUsageViewModels,
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController
    with I18nSupport {

  def index(dbId: Int, filter: Option[String]): Action[AnyContent] = authAction.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      val recordsF = OptionT.liftF(filter match {
        case Some("only-empty") => getSqlQueries.findEmptyCaln(dbId)
        case _                  => getSqlQueries.getSourRecords(dbId)
      })
      (for {
        database <- OptionT(genealogyDatabaseService.getGenealogyDatabase(dbId))
        records  <- recordsF
      } yield {
        Ok(sourRecordsListView(Some(database), records.toList, filterOption = filter))
      }).getOrElse(NotFound(s"Genealogy database $dbId not found"))
  }

  def showSourCitationsFromRecord(dbId: Int, sourRecordId: Int): Action[AnyContent] = authAction.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      (for {
        database                <- OptionT(genealogyDatabaseService.getGenealogyDatabase(dbId))
        sourRecord              <- getSqlQueries.getSourRecord(dbId, sourRecordId)
        sourCitations           <- OptionT.liftF(getSqlQueries.getSourCitationsFromRecord(sourRecordId))
        repository              <- sourRecord.repoId.traverse(getSqlQueries.getRepository(dbId, _))
        sourCitationsWithUsages <-
          OptionT.liftF(sourCitations.traverse { sourCitation =>
            for {
              usages      <- getCitationUsagesSqlQueries.getSourCitationUsages(sourCitation.id)
              usagesModel <- gedcomObjectUsageViewModels.buildViewModel(dbId, usages)
            } yield {
              SourCitationViewModel(sourCitation = sourCitation, usages = usagesModel)
            }
          })
      } yield {
        Ok(sourCitationsFromRecordListView(Some(database), sourRecord, repository, sourCitationsWithUsages))
      }).getOrElse(NotFound("database or source record not found"))
  }

}
