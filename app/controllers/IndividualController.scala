package controllers

import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthAction
import models.AuthenticatedRequest
import models.Person
import play.api.i18n.*
import play.api.mvc.*
import services.PersonService
import services.SessionService
import views.html.Individual

@Singleton
class IndividualController @Inject() (
    authAction: AuthAction,
    personService: PersonService,
    sessionService: SessionService,
    individualView: Individual,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def showPerson(baseId: Int, id: Int): Action[AnyContent] = authAction.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      personService.getPerson(id).map { (personOption: Option[Person]) =>
        personOption.fold(NotFound("Nothing here")) { person =>
          val isAllowedToSee = authenticatedRequest.localSession.sessionData.userData.fold(false)(_.seePrivacy)

          if (person.details.privacyRestriction.isEmpty || isAllowedToSee) {
            sessionService.insertPersonInHistory(person)
            Ok(individualView(person, baseId))
          } else {
            Forbidden("Not allowed")
          }
        }
      }
  }

  def languageSwitch(lang: String): Action[AnyContent] = authAction.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      Future.successful(Redirect(routes.IndividualController.showPerson(1, 300)).withLang(Lang(lang)))
  }
}
