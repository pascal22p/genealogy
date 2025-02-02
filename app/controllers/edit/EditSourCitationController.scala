package controllers.edit

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthJourney
import models.forms.SourCitationForm
import models.SourCitation
import models.SourCitationType
import models.SourCitationType.EventSourCitation
import models.SourCitationType.IndividualSourCitation
import models.SourCitationType.UnknownSourCitation
import models.SourRecord
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import play.api.mvc.Result
import play.api.Logging
import queries.GetSqlQueries
import queries.UpdateSqlQueries
import services.SourCitationService
import views.html.edit.EditSourCitation
import views.html.ServiceUnavailable

@Singleton
class EditSourCitationController @Inject() (
    authJourney: AuthJourney,
    sourCitationService: SourCitationService,
    updateSqlQueries: UpdateSqlQueries,
    getSqlQueries: GetSqlQueries,
    sourCitationView: EditSourCitation,
    serviceUnavailableView: ServiceUnavailable,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport
    with Logging {

  private def handleSourCitation(id: Int, dbId: Int)(
      block: (SourCitation, List[SourRecord]) => Future[Result]
  ): Future[Result] = {
    for {
      sourCitationList <- sourCitationService.getSourCitations(id, UnknownSourCitation, dbId)
      sourRecords      <- getSqlQueries.getAllSourRecords
    } yield {
      sourCitationList.headOption.fold(Future.successful(NotFound("SourCitation could not be found")))(
        block(_, sourRecords)
      )
    }
  }.flatten

  def showForm(baseId: Int, id: Int) = authJourney.authWithAdminRight.async { implicit request =>
    handleSourCitation(id, baseId) { (sourCitation, sourRecords) =>
      val form = SourCitationForm.sourCitationForm.fill(sourCitation.toForm)
      Future.successful(Ok(sourCitationView(baseId, form, sourCitation, sourRecords)))
    }
  }

  def onSubmit(baseId: Int, id: Int) = authJourney.authWithAdminRight.async { implicit request =>
    def errorFunction(formWithErrors: Form[SourCitationForm]): Future[Result] = {
      handleSourCitation(id, baseId) { (sourCitation, sourRecords) =>
        Future.successful(BadRequest(sourCitationView(baseId, formWithErrors, sourCitation, sourRecords)))
      }
    }

    val successFunction: SourCitationForm => Future[Result] = { dataForm =>
      handleSourCitation(id, baseId) { (sourCitation, _) =>
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
