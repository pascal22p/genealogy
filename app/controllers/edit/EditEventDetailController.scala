package controllers.edit

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthJourney
import cats.implicits.*
import models.forms.EventDetailForm
import models.AuthenticatedRequest
import models.EventDetail
import models.Person
import models.Place
import models.ResnType.PrivacyResn
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import play.api.mvc.Result
import queries.GetSqlQueries
import queries.UpdateSqlQueries
import services.EventService
import services.PersonService
import services.SessionService
import views.html.edit.EditEventDetail
import views.html.ServiceUnavailable

@Singleton
class EditEventDetailController @Inject() (
    authJourney: AuthJourney,
    eventService: EventService,
    personService: PersonService,
    sessionService: SessionService,
    getSqlQueries: GetSqlQueries,
    updateSqlQueries: UpdateSqlQueries,
    editEventDetail: EditEventDetail,
    serviceUnavailableView: ServiceUnavailable,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def showForm(baseId: Int, id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async { implicit request =>
    handleEvent(id) { (event, _, allPlace) =>
      val form = EventDetailForm.eventDetailForm.fill(event.toForm)
      Future.successful(Ok(editEventDetail(baseId, form, allPlace, event)))
    }
  }

  def onSubmit(baseId: Int, id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async { implicit request =>
    def errorFunction: Form[EventDetailForm] => Future[Result] = { (formWithErrors: Form[EventDetailForm]) =>
      handleEvent(id) { (event, _, allPlace) =>
        Future.successful(BadRequest(editEventDetail(baseId, formWithErrors, allPlace, event)))
      }
    }

    val successFunction: EventDetailForm => Future[Result] = { (dataForm: EventDetailForm) =>
      handleEvent(id) { (event, _, allPlace) =>
        updateSqlQueries.updateEventDetails(event.fromForm(dataForm, allPlace)).map {
          case 1 => Redirect(controllers.routes.EventController.showEvent(baseId, id))
          case _ => InternalServerError(serviceUnavailableView("No record was updated"))
        }
      }
    }

    val formValidationResult = EventDetailForm.eventDetailForm.bindFromRequest()
    formValidationResult.fold(errorFunction, successFunction)
  }

  private def handleEvent(id: Int)(
      result: (EventDetail, Option[Person], List[Place]) => Future[Result]
  )(implicit request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    eventService.getEvent(id).flatMap { eventOption =>
      getSqlQueries.getAllPlaces.flatMap { allPlace =>
        eventOption.fold(Future.successful(NotFound("Event could not be found"))) { event =>
          event.ownerId.traverse(personId => personService.getPerson(personId)).flatMap { person =>
            val isAllowedToSee = request.localSession.sessionData.userData.fold(false)(_.seePrivacy)

            if (!event.privacyRestriction.contains(PrivacyResn) || isAllowedToSee) {
              person.flatten.map(sessionService.insertPersonInHistory)
              result(event, person.flatten, allPlace)
            } else {
              Future.successful(Forbidden("Not allowed"))
            }
          }
        }
      }
    }
  }
}
