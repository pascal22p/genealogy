package controllers.edit

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthJourney
import models.forms.SourCitationForm
import models.SourCitation
import models.SourCitationType.EventSourCitation
import models.SourCitationType.IndividualSourCitation
import models.SourCitationType.UnknownSourCitation
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import play.api.mvc.Result
import play.api.Logging
import queries.UpdateSqlQueries
import services.SourCitationService
import views.html.edit.EditSourCitation
import views.html.ServiceUnavailable

@Singleton
class EditSourCitationController @Inject() (
    authJourney: AuthJourney,
    sourCitationService: SourCitationService,
    updateSqlQueries: UpdateSqlQueries,
    sourCitationView: EditSourCitation,
    serviceUnavailableView: ServiceUnavailable,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport
    with Logging {

  private def handleSourCitation(baseId: Int, id: Int)(
      block: SourCitation => Future[Result]
  ): Future[Result] = {
    sourCitationService.getSourCitations(id, UnknownSourCitation, baseId).flatMap { sourCitationList =>
      sourCitationList.headOption.fold(Future.successful(NotFound("SourCitation could not be found")))(block)
    }
  }

  def showForm(baseId: Int, id: Int) = authJourney.authWithAdminRight.async { implicit request =>
    handleSourCitation(baseId, id) { sourCitation =>
      val form = SourCitationForm.sourCitationForm.fill(sourCitation.toForm)
      sourCitationService.getSourRecords(baseId).map { sourRecords =>
        Ok(sourCitationView(baseId, form, sourCitation, sourRecords))
      }
    }
  }

  def onSubmit(baseId: Int, id: Int) = authJourney.authWithAdminRight.async { implicit request =>
    def errorFunction(formWithErrors: Form[SourCitationForm]): Future[Result] = {
      handleSourCitation(baseId, id) { sourCitation =>
        sourCitationService.getSourRecords(baseId).map { sourRecords =>
          BadRequest(sourCitationView(baseId, formWithErrors, sourCitation, sourRecords))
        }
      }
    }

    val successFunction: SourCitationForm => Future[Result] = { dataForm =>
      handleSourCitation(baseId, id) { sourCitation =>
        updateSqlQueries.updateSourCitation(sourCitation.fromForm(dataForm)).map {
          case 1 =>
            sourCitation.sourceType match {
              case _: EventSourCitation.type =>
                sourCitation.ownerId.fold(NotFound("Record updated but parent not found"))(eventId =>
                  Redirect(controllers.routes.EventController.showEvent(baseId, eventId))
                )
              case _: IndividualSourCitation.type =>
                sourCitation.ownerId.fold(NotFound("Record updated but parent not found"))(personId =>
                  Redirect(controllers.routes.IndividualController.showPerson(baseId, personId))
                )
              // case FamilySourCitation => Redirect(controllers.routes.FamilyController.showFamily(sourCitation.ownerId.get))
              case _ => NotImplemented(serviceUnavailableView("Record updated no view implemented yet"))
            }
          case _ => InternalServerError(serviceUnavailableView("No record was updated"))
        }
      }
    }

    val formValidationResult = SourCitationForm.sourCitationForm.bindFromRequest()
    formValidationResult.fold(errorFunction, successFunction)
  }

}
