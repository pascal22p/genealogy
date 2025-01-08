package controllers.edit

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthAction
import actions.AuthJourney
import cats.implicits.*
import models.forms.SourCitationForm
import models.AuthenticatedRequest
import models.SourCitation
import models.SourCitationType
import models.SourCitationType.EventSourCitation
import models.SourCitationType.FamilySourCitation
import models.SourCitationType.IndividualSourCitation
import models.SourCitationType.UnknownSourCitation
import org.apache.pekko.http.scaladsl.model.HttpCharsetRange.*
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import play.api.mvc.Result
import play.api.Logging
import queries.GetSqlQueries
import queries.UpdateSqlQueries
import services.EventService
import services.PersonService
import services.SessionService
import services.SourCitationService
import views.html.edit.EditSourCitation
import views.html.ServiceUnavailable

@Singleton
class EditSourCitationController @Inject() (
    authJourney: AuthJourney,
    eventService: EventService,
    personService: PersonService,
    sourCitationService: SourCitationService,
    sessionService: SessionService,
    getSqlQueries: GetSqlQueries,
    updateSqlQueries: UpdateSqlQueries,
    sourCitationView: EditSourCitation,
    serviceUnavailableView: ServiceUnavailable,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport
    with Logging {

  private def handleSourCitation(id: Int)(
      block: SourCitation => Future[Result]
  )(implicit request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    sourCitationService.getSourCitations(id, UnknownSourCitation).flatMap { sourCitationList =>
      sourCitationList.headOption.fold(Future.successful(NotFound("SourCitation could not be found")))(block)
    }
  }

  def showForm(baseId: Int, id: Int) = authJourney.authWithAdminRight.async { implicit request =>
    handleSourCitation(id) { sourCitation =>
      val form = SourCitationForm.sourCitationForm.fill(sourCitation.toForm)
      Future.successful(Ok(sourCitationView(baseId, form, sourCitation)))
    }
  }

  def onSubmit(baseId: Int, id: Int) = authJourney.authWithAdminRight.async { implicit request =>
    def errorFunction(formWithErrors: Form[SourCitationForm]): Future[Result] = {
      handleSourCitation(id) { sourCitation =>
        Future.successful(BadRequest(sourCitationView(baseId, formWithErrors, sourCitation)))
      }
    }

    val successFunction: SourCitationForm => Future[Result] = { dataForm =>
      handleSourCitation(id) { sourCitation =>
        updateSqlQueries.updateSourCitation(sourCitation.fromForm(dataForm)).map {
          case 1 =>
            sourCitation.sourceType match {
              case EventSourCitation =>
                sourCitation.ownerId.fold(NotFound("Record updated but parent not found"))(eventId =>
                  Redirect(controllers.routes.EventController.showEvent(baseId, eventId))
                )
              case IndividualSourCitation =>
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
