package controllers.edit

import javax.inject.{Singleton, Inject}
import actions.AuthAction
import services.PersonService
import services.SessionService
import queries.MariadbQueries
import views.html.ServiceUnavailable
import views.html.edit.EditEventDetails
import play.api.mvc.ControllerComponents
import scala.concurrent.ExecutionContext
import play.api.mvc.BaseController
import play.api.i18n.I18nSupport
import play.api.Logging
import scala.concurrent.Future
import services.EventService
import cats.implicits.*
import models.forms.EventDetailForm
import play.api.data.Form
import play.api.mvc.Result
import actions.AuthJourney

@Singleton
class EditEventDetailsController @Inject()(
    authJourney: AuthJourney,
    eventService: EventService,
    personService: PersonService,
    sessionService: SessionService,
    mariadbQueries: MariadbQueries,
    editEventDetails: EditEventDetails,
    serviceUnavailableView: ServiceUnavailable,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport
    with Logging {

  def showForm(id: Int) = authJourney.authWithAdminRight.async { implicit request =>
    eventService.getEvent(id).flatMap { eventOption =>
      mariadbQueries.getAllPlaces.flatMap { allPlace =>
      eventOption.fold(Future.successful(NotFound("Event could not be found"))) { event =>
        event.ownerId.traverse(personId => personService.getPerson(personId)).map { person =>
          val isAllowedToSee = request.localSession.sessionData.userData.fold(false)(_.seePrivacy)

          if (!event.privacyRestriction.contains("privacy") || isAllowedToSee) {
            person.flatten.map(sessionService.insertPersonInHistory)
            val form = EventDetailForm.eventDetailForm.fill(event.toForm)
            Ok(editEventDetails(form, allPlace))
          } else {
            Forbidden("Not allowed")
          }
        }
      }
    }
  }
  }


  def onSubmit = authJourney.authWithAdminRight.async { implicit request =>
    def errorFunction: Form[EventDetailForm] => Future[Result] = { (formWithErrors: Form[EventDetailForm]) =>
      // This is the bad case, where the form had validation errors.
      // Let's show the user the form again, with the errors highlighted.
      // Note how we pass the form with errors to the template.
      mariadbQueries.getAllPlaces.map { allPlace =>
        BadRequest(editEventDetails(formWithErrors, allPlace))
      }
    }

    val successFunction: EventDetailForm => Future[Result] = { (dataForm: EventDetailForm) =>
      mariadbQueries.updateEventDetails(dataForm).map {
        case 1 => Redirect(controllers.routes.EventController.showEvent(dataForm.events_details_id))
        case _ => InternalServerError(serviceUnavailableView("No record was updated"))
      }
    }

    val formValidationResult = EventDetailForm.eventDetailForm.bindFromRequest()
    formValidationResult.fold(errorFunction, successFunction)
  }
        
  
}
