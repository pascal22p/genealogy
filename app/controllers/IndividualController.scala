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
import views.html.Individual

@Singleton
class IndividualController @Inject() (
    authAction: AuthAction,
    personService: PersonService,
    individualView: Individual,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def showPerson(id: Int): Action[AnyContent] = authAction.async { implicit request: AuthenticatedRequest[AnyContent] =>
    personService.getPerson(id).map { (personOption: Option[Person]) =>
      personOption.fold(NotFound("Nothing here")) { person =>
        if (person.details.privacyRestriction.contains("privacy")) {
          Forbidden("Not allowed")
        } else {
          Ok(individualView(person, request.localSession.sessionData.dbId))
        }
      }
    }
  }

  def languageSwitch(lang: String): Action[AnyContent] = authAction.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      Future.successful(Redirect(routes.IndividualController.showPerson(300)).withLang(Lang(lang)))

  }
}
