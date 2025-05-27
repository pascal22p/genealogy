package controllers.delete

import javax.inject.*

import scala.concurrent.ExecutionContext

import actions.AuthJourney
import cats.data.OptionT
import cats.implicits.*
import models.AuthenticatedRequest
import models.EventType.UnknownEvent
import models.Events
import play.api.i18n.*
import play.api.mvc.*
import queries.DeleteSqlQueries
import services.EventService
import views.html.delete.DeleteEventView

@Singleton
class DeleteEventController @Inject() (
    authJourney: AuthJourney,
    deleteEventView: DeleteEventView,
    deleteSqlQueries: DeleteSqlQueries,
    eventService: EventService,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def deleteEventConfirmation(baseId: Int, id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      OptionT(eventService.getEvent(id)).fold(NotFound("Nothing here")) { event =>
        Ok(deleteEventView(baseId, Events(List(event), None, UnknownEvent)))
      }
  }

  def deleteEventAction(baseId: Int, id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      deleteSqlQueries.deleteEvent(id).map { _ =>
        Redirect(controllers.routes.HomeController.showSurnames(baseId))
      }
  }

}
