package controllers

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthAction
import cats.*
import cats.implicits.*
import models.AuthenticatedRequest
import models.Child
import models.Events
import models.Family
import models.Person
import models.PersonDetails
import models.Sex
import models.UnknownSex
import play.api.i18n.*
import play.api.mvc.*
import play.api.Logging
import services.DescendanceService
import services.PersonService
import services.SessionService
import views.html.Descendants
import views.html.Individual

@Singleton
class DescendanceController @Inject() (
    authAction: AuthAction,
    personService: PersonService,
    descendanceService: DescendanceService,
    sessionService: SessionService,
    individualView: Individual,
    descendantsView: Descendants,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport
    with Logging {

  def showDescendant(id: Int): Action[AnyContent] = authAction.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      descendanceService.getDescendant(id, 0).map {
        case Some(tree) =>
          sessionService.insertPersonInHistory(tree.copy(families = List.empty[Family]))
          Ok(descendantsView(tree, 1))
        case None => NotFound("Nothing here")
      }
  }
}
