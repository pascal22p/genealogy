package controllers.edit

import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthJourney
import models.forms.PersonDetailsForm
import models.AuthenticatedRequest
import models.Person
import models.ResnType.PrivacyResn
import play.api.data.Form
import play.api.i18n.*
import play.api.mvc.*
import queries.UpdateSqlQueries
import services.PersonService
import services.SessionService
import views.html.edit.EditPersonDetails
import views.html.ServiceUnavailable

@Singleton
class EditPersonDetailsController @Inject() (
    authJourney: AuthJourney,
    personService: PersonService,
    sessionService: SessionService,
    updateSqlQueries: UpdateSqlQueries,
    editPersonDetails: EditPersonDetails,
    serviceUnavailableView: ServiceUnavailable,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def showForm(baseId: Int, id: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      personService.getPerson(id).map { (personOption: Option[Person]) =>
        personOption.fold(NotFound("Nothing here")) { person =>
          val isAllowedToSee = authenticatedRequest.localSession.sessionData.userData.fold(false)(_.seePrivacy)

          if (!person.details.privacyRestriction.contains(PrivacyResn) || isAllowedToSee) {
            sessionService.insertPersonInHistory(person)
            val form = PersonDetailsForm.personDetailsForm.fill(person.details.toForm)
            Ok(editPersonDetails(baseId, form))
          } else {
            Forbidden("Not allowed")
          }
        }
      }
  }

  def onSubmit(baseId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest =>
      val errorFunction: Form[PersonDetailsForm] => Future[Result] = { (formWithErrors: Form[PersonDetailsForm]) =>
        // This is the bad case, where the form had validation errors.
        // Let's show the user the form again, with the errors highlighted.
        // Note how we pass the form with errors to the template.
        Future.successful(BadRequest(editPersonDetails(baseId, formWithErrors)))
      }

      val successFunction: PersonDetailsForm => Future[Result] = { (dataForm: PersonDetailsForm) =>
        updateSqlQueries.updatePersonDetails(dataForm.toPersonalDetails).map {
          case 1 => Redirect(controllers.routes.IndividualController.showPerson(baseId, dataForm.id))
          case _ => InternalServerError(serviceUnavailableView("No record was updated"))
        }
      }

      val formValidationResult = PersonDetailsForm.personDetailsForm.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }

}
