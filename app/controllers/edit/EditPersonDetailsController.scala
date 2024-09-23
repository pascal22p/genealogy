package controllers.edit

import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthAction
import models.AuthenticatedRequest
import models.Person
import models.forms.PersonDetailsForm
import play.api.i18n.*
import play.api.mvc.*
import services.PersonService
import services.SessionService
import views.html.edit.EditPersonDetails
import play.api.data.Form
import queries.MariadbQueries
import play.api.Logging
import views.html.ServiceUnavailable

@Singleton
class EditPersonDetailsController @Inject() (
    authAction: AuthAction,
    personService: PersonService,
    sessionService: SessionService,
    mariadbQueries: MariadbQueries,
    editPersonDetails: EditPersonDetails,
    serviceUnavailableView: ServiceUnavailable,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport
    with Logging {

  def showForm(id: Int): Action[AnyContent] = authAction.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      personService.getPerson(id).map { (personOption: Option[Person]) =>
        personOption.fold(NotFound("Nothing here")) { person =>
          val isAllowedToSee = authenticatedRequest.localSession.sessionData.userData.fold(false)(_.seePrivacy)

          if (!person.details.privacyRestriction.contains("privacy") || isAllowedToSee) {
            sessionService.insertPersonInHistory(person)
            val form = PersonDetailsForm.personDetailsForm.fill(person.details.toForm)
            Ok(editPersonDetails(form))
          } else {
            Forbidden("Not allowed")
          }
        }
      }
  }

  def onSubmit: Action[AnyContent] = authAction.async { implicit authenticatedRequest =>
    val errorFunction: Form[PersonDetailsForm] => Future[Result] = { (formWithErrors: Form[PersonDetailsForm]) =>
      // This is the bad case, where the form had validation errors.
      // Let's show the user the form again, with the errors highlighted.
      // Note how we pass the form with errors to the template.
      logger.error(s"XXXXXXXX $formWithErrors")
      Future.successful(BadRequest(editPersonDetails(formWithErrors)))
    }

    val successFunction: PersonDetailsForm => Future[Result] = { (dataForm: PersonDetailsForm) =>
      mariadbQueries.updatePersonDetails(dataForm.toPersonalDetails).map { 
        case 1 => Redirect(controllers.routes.IndividualController.showPerson(dataForm.id))
        case _ => InternalServerError(serviceUnavailableView("No record was updated")) 
      }
    }

    val formValidationResult = PersonDetailsForm.personDetailsForm.bindFromRequest()
    formValidationResult.fold(errorFunction, successFunction)
  }

}
