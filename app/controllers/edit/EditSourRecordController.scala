package controllers.edit

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthJourney
import models.forms.SourRecordForm
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
import play.api.Logging
import queries.UpdateSqlQueries
import services.SourCitationService
import services.SourRecordService
import views.html.edit.EditSourRecord
import views.html.ServiceUnavailable

@Singleton
class EditSourRecordController @Inject() (
    authJourney: AuthJourney,
    sourRecordService: SourRecordService,
    sourCitationService: SourCitationService,
    updateSqlQueries: UpdateSqlQueries,
    sourRecordView: EditSourRecord,
    serviceUnavailableView: ServiceUnavailable,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport
    with Logging {

  private def handleSourRecord(id: Int)(
      block: SourRecord => Future[Result]
  ): Future[Result] = {
    sourRecordService.getSourRecord(id).flatMap { sourRecord =>
      sourRecord.fold(Future.successful(NotFound("SourCitation could not be found")))(block)
    }
  }

  def showForm(baseId: Int, sourRecordId: Int, sourCitationType: SourCitationType, sourCitationId: Int) =
    authJourney.authWithAdminRight.async { implicit request =>
      handleSourRecord(sourRecordId) { sourRecord =>
        val form = SourRecordForm.sourRecordForm.fill(sourRecord.toForm(sourCitationId, sourCitationType))
        Future.successful(Ok(sourRecordView(baseId, form, sourRecord)))
      }
    }

  def onSubmit(baseId: Int, sourRecordId: Int) = authJourney.authWithAdminRight.async { implicit request =>
    def errorFunction(formWithErrors: Form[SourRecordForm]): Future[Result] = {
      handleSourRecord(sourRecordId) { sourRecord =>
        Future.successful(BadRequest(sourRecordView(baseId, formWithErrors, sourRecord)))
      }
    }

    val successFunction: SourRecordForm => Future[Result] = { dataForm =>
      handleSourRecord(sourRecordId) { sourRecord =>
        updateSqlQueries.updateSourRecord(sourRecord.fromForm(dataForm)).flatMap {
          case 1 =>
            dataForm.parentType match {
              case _: EventSourCitation.type =>
                sourCitationService.getSourCitations(dataForm.parentId, UnknownSourCitation).map { sourCitationList =>
                  sourCitationList.headOption.fold(NotFound("SourCitation could not be found")) { sourCitation =>
                    Redirect(controllers.routes.EventController.showEvent(baseId, sourCitation.ownerId.getOrElse(0)))
                  }
                }
              case _: IndividualSourCitation.type =>
                sourCitationService.getSourCitations(dataForm.parentId, UnknownSourCitation).map { sourCitationList =>
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
