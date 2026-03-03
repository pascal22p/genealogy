package controllers

import javax.inject.*

import scala.concurrent.ExecutionContext

import actions.AuthAction
import cats.data.OptionT
import cats.implicits.*
import models.AuthenticatedRequest
import models.ResnType.PrivacyResn
import play.api.i18n.*
import play.api.mvc.*
import services.EventService
import services.GenealogyDatabaseService
import services.PersonService
import services.SessionService
import views.html.Event

@Singleton
class EventController @Inject() (
    authAction: AuthAction,
    genealogyDatabaseService: GenealogyDatabaseService,
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
      (for {
        database <- OptionT(genealogyDatabaseService.getGenealogyDatabase(baseId))
        event    <- OptionT(eventService.getEvent(id))
        person   <- OptionT(event.ownerId.flatTraverse(personId => personService.getPerson(personId)))
      } yield {
        val isAllowedToSee = authenticatedRequest.localSession.sessionData.userData.fold(false)(_.seePrivacy)

        if (!event.privacyRestriction.contains(PrivacyResn) || isAllowedToSee) {
          sessionService.insertPersonInHistory(person)
          Ok(eventView(event, Some(database)))
        } else {
          Forbidden("Not allowed")
        }
      }).getOrElse(NotFound("database, person or event not found"))
  }
}
