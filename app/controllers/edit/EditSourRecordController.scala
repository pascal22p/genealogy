package controllers.edit

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthAction
import actions.AuthJourney
import cats.implicits.*
import models.forms.SourRecordForm
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import play.api.mvc.Result
import play.api.Logging
import queries.GetSqlQueries
import queries.UpdateSqlQueries
import services.SourRecordService
import services.PersonService
import services.SessionService
import views.html.edit.EditSourRecord
import views.html.ServiceUnavailable
import services.SourCitationService
import models.SourRecord
import models.AuthenticatedRequest
import play.api.mvc.AnyContent
import play.api.data.FormError
import models.SourCitationType.SourCitationType
import models.SourCitationType.EventSourCitation
import models.SourCitationType.IndividualSourCitation
import models.SourCitationType.FamilySourCitation
import models.SourCitationType.UnknownSourCitation

@Singleton
class EditSourRecordController @Inject() (
    authJourney: AuthJourney,
    personService: PersonService,
    sourRecordService: SourRecordService,
    sourCitationService: SourCitationService,
    sessionService: SessionService,
    getSqlQueries: GetSqlQueries,
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
  )(implicit request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    sourRecordService.getSourRecord(id).flatMap { sourRecord =>
      sourRecord.fold(Future.successful(NotFound("SourCitation could not be found")))(block)
    }
  }

  def showForm(sourRecordId: Int, sourCitationType: SourCitationType, sourCitationId: Int) = authJourney.authWithAdminRight.async { implicit request =>
    handleSourRecord(sourRecordId) { sourRecord =>
      val form = SourRecordForm.sourRecordForm.fill(sourRecord.toForm(sourCitationId, sourCitationType))
      Future.successful(Ok(sourRecordView(form, sourRecord)))
    }
  }

  def onSubmit(sourRecordId: Int) = authJourney.authWithAdminRight.async { implicit request =>
    def errorFunction(formWithErrors: Form[SourRecordForm]): Future[Result] = {
      handleSourRecord(sourRecordId) { sourRecord =>
        Future.successful(BadRequest(sourRecordView(formWithErrors, sourRecord)))
      }
    }

    val successFunction: SourRecordForm => Future[Result] = { dataForm =>
      handleSourRecord(sourRecordId) { sourRecord =>
        updateSqlQueries.updateSourRecord(sourRecord.fromForm(dataForm)).flatMap {
          case 1 => dataForm.parentType match {
            case EventSourCitation => 
              sourCitationService.getSourCitations(dataForm.parentId, UnknownSourCitation).map { sourCitationList =>
                sourCitationList.headOption.fold(NotFound("SourCitation could not be found")) { sourCitation =>
                  Redirect(controllers.routes.EventController.showEvent(sourCitation.ownerId.getOrElse(0)))
                }
              }
            case IndividualSourCitation => 
              sourCitationService.getSourCitations(dataForm.parentId, UnknownSourCitation).map { sourCitationList =>
                sourCitationList.headOption.fold(NotFound("SourCitation could not be found")) { sourCitation =>
                  Redirect(controllers.routes.IndividualController.showPerson(sourCitation.ownerId.getOrElse(0)))
                }
              }
            case FamilySourCitation => Future.successful(NotImplemented(serviceUnavailableView("Family edit Not implemented")))
            case UnknownSourCitation => Future.successful(InternalServerError(serviceUnavailableView("Unknown sour citation type")))
          }
          case _ => Future.successful(InternalServerError(serviceUnavailableView("No record was updated")))
        }
      }
    }

    val formValidationResult = SourRecordForm.sourRecordForm.bindFromRequest()
    formValidationResult.fold(errorFunction, successFunction)
  }

}