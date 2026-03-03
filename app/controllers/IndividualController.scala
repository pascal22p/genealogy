package controllers

import javax.inject.*

import scala.concurrent.ExecutionContext

import actions.AuthAction
import models.AuthenticatedRequest
import cats.data.OptionT
import play.api.i18n.*
import play.api.mvc.*
import services.GenealogyDatabaseService
import services.PersonService
import services.SessionService
import views.html.Individual

@Singleton
class IndividualController @Inject() (
    authAction: AuthAction,
    personService: PersonService,
    sessionService: SessionService,
    individualView: Individual,
    genealogyDatabaseService: GenealogyDatabaseService,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def showPerson(baseId: Int, id: Int): Action[AnyContent] = authAction.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      (for {
        database <- OptionT(genealogyDatabaseService.getGenealogyDatabase(baseId))
        person   <- OptionT(personService.getPerson(id))
      } yield {
        val isAllowedToSee = authenticatedRequest.localSession.sessionData.userData.fold(false)(_.seePrivacy)

        if (person.details.privacyRestriction.isEmpty || isAllowedToSee) {
          sessionService.insertPersonInHistory(person)
          Ok(individualView(person, Some(database)))
        } else {
          Forbidden("Not allowed")
        }
      }).getOrElse(NotFound("database or person not found"))
  }
}
