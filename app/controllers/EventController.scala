package controllers

import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthAction
import models.AuthenticatedRequest
import models.EventDetail
import models.Person
import play.api.i18n.*
import play.api.mvc.*
import services.EventService
import services.PersonService
import views.html.Event

@Singleton
class EventController @Inject() (
    authAction: AuthAction,
    eventService: EventService,
    eventView: Event,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def showEvent(id: Int): Action[AnyContent] = authAction.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      eventService.getEvent(id).map { eventOption =>
        eventOption.fold(NotFound("Event could not be found")) { event =>
          if (event.privacyRestriction.contains("privacy")) {
            authenticatedRequest.localSession.sessionData.userData.fold(Forbidden("Not allowed")) { userData =>
              if (userData.seePrivacy)
                Ok(eventView(event, authenticatedRequest.localSession.sessionData.dbId))
              else
                Forbidden("Not allowed")
            }
          } else {
            Ok(eventView(event, authenticatedRequest.localSession.sessionData.dbId))
          }
        }
      }
  }

}
