package controllers

import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthAction
import cats.implicits.*
import models.AuthenticatedRequest
import models.EventDetail
import models.EventType.IndividualEvent
import models.Person
import play.api.i18n.*
import play.api.mvc.*
import services.EventService
import services.PersonService
import services.SessionService
import views.html.Event

@Singleton
class EventController @Inject() (
    authAction: AuthAction,
    eventService: EventService,
    personService: PersonService,
    sessionService: SessionService,
    eventView: Event,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def showEvent(baseId: Int, id: Int): Action[AnyContent] = authAction.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      eventService.getEvent(id).flatMap { eventOption =>
        eventOption.fold(Future.successful(NotFound("Event could not be found"))) { event =>
          event.ownerId.traverse(personId => personService.getPerson(personId)).map { person =>
            val isAllowedToSee = authenticatedRequest.localSession.sessionData.userData.fold(false)(_.seePrivacy)

            if (!event.privacyRestriction.contains("privacy") || isAllowedToSee) {
              person.flatten.map(sessionService.insertPersonInHistory)
              Ok(eventView(event, baseId, person.flatten))
            } else {
              Forbidden("Not allowed")
            }
          }
        }
      }
  }
}
