package controllers.edit

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthJourney
import models.forms.SourRecordForm
import models.queryData.RepositoryQueryData
import models.GenealogyDatabase
import models.SourCitationType.EventSourCitation
import models.SourCitationType.FamilySourCitation
import models.SourCitationType.IndividualSourCitation
import models.SourCitationType.SourCitationType
import models.SourCitationType.UnknownSourCitation
import models.SourRecord
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import play.api.mvc.Result
import queries.GetSqlQueries
import queries.UpdateSqlQueries
import services.GenealogyDatabaseService
import services.SourCitationService
import services.SourRecordService
import views.html.edit.EditSourRecord
import views.html.ServiceUnavailable

@Singleton
class EditSourRecordController @Inject() (
    authJourney: AuthJourney,
    sourRecordService: SourRecordService,
    sourCitationService: SourCitationService,
    genealogyDatabaseService: GenealogyDatabaseService,
    getSqlQueries: GetSqlQueries,
    updateSqlQueries: UpdateSqlQueries,
    sourRecordView: EditSourRecord,
    serviceUnavailableView: ServiceUnavailable,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  private def handleSourRecord(dbId: Int, id: Int)(
      block: (SourRecord, Option[GenealogyDatabase], List[RepositoryQueryData]) => Future[Result]
  ): Future[Result] =
    sourRecordService
      .getSourRecord(dbId, id)
      .foldF(Future.successful(NotFound("SourCitation could not be found"))) { sourRecord =>
        for {
          database     <- genealogyDatabaseService.getGenealogyDatabase(dbId)
          repositories <- getSqlQueries.getRepositories(dbId)
          result       <- block(sourRecord, database, repositories.sortBy(_.name))
        } yield result
      }

  def showForm(baseId: Int, sourRecordId: Int, sourCitationType: SourCitationType, sourCitationId: Int) =
    authJourney.authWithAdminRight.async { implicit request =>
      handleSourRecord(baseId, sourRecordId) { (sourRecord, database, repositories) =>
        val form = SourRecordForm.sourRecordForm.fill(sourRecord.toForm(sourCitationId, sourCitationType))
        Future.successful(Ok(sourRecordView(database, form, sourRecord, repositories)))
      }
    }

  def onSubmit(baseId: Int, sourRecordId: Int) = authJourney.authWithAdminRight.async { implicit request =>
    def errorFunction(formWithErrors: Form[SourRecordForm]): Future[Result] = {
      handleSourRecord(baseId, sourRecordId) { (sourRecord, database, repositories) =>
        Future.successful(BadRequest(sourRecordView(database, formWithErrors, sourRecord, repositories)))
      }
    }

    val successFunction: SourRecordForm => Future[Result] = { dataForm =>
      handleSourRecord(baseId, sourRecordId) { (sourRecord, _, _) =>
        updateSqlQueries.updateSourRecord(sourRecord.fromForm(dataForm)).flatMap {
          case 1 =>
            dataForm.parentType match {
              case _: EventSourCitation.type =>
                sourCitationService.getSourCitations(dataForm.parentId, UnknownSourCitation, baseId).map {
                  sourCitationList =>
                    sourCitationList.headOption.fold(NotFound("SourCitation could not be found")) { sourCitation =>
                      Redirect(controllers.routes.EventController.showEvent(baseId, sourCitation.ownerId.getOrElse(0)))
                    }
                }
              case _: IndividualSourCitation.type =>
                sourCitationService.getSourCitations(dataForm.parentId, UnknownSourCitation, baseId).map {
                  sourCitationList =>
                    sourCitationList.headOption.fold(NotFound("SourCitation could not be found")) { sourCitation =>
                      Redirect(
                        controllers.routes.IndividualController.showPerson(baseId, sourCitation.ownerId.getOrElse(0))
                      )
                    }
                }
              case _: FamilySourCitation.type =>
                Future.successful(NotImplemented(serviceUnavailableView("Family edit Not implemented")))
              case _: UnknownSourCitation.type =>
                Future.successful(InternalServerError(serviceUnavailableView("Unknown sour citation type")))
            }
          case _ => Future.successful(InternalServerError(serviceUnavailableView("No record was updated")))
        }
      }
    }

    val formValidationResult = SourRecordForm.sourRecordForm.bindFromRequest()
    formValidationResult.fold(errorFunction, successFunction)
  }

}
