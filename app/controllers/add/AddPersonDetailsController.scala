package controllers.add

import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthJourney
import models.forms.PersonDetailsForm
import models.AuthenticatedRequest
import play.api.data.Form
import play.api.i18n.*
import play.api.mvc.*
import queries.InsertSqlQueries
import views.html.add.AddPersonDetails
import views.html.ServiceUnavailable

@Singleton
class AddPersonDetailsController @Inject() (
    authJourney: AuthJourney,
    insertSqlQueries: InsertSqlQueries,
    addPersonDetails: AddPersonDetails,
    serviceUnavailableView: ServiceUnavailable,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def showForm(baseId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      val filled =
        PersonDetailsForm(baseId, -1, "", "", "", "", "", "", "", "", None)
      val form = PersonDetailsForm.personDetailsForm.fill(filled)
      Future.successful(Ok(addPersonDetails(baseId, form)))
  }

  def onSubmit(baseId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest =>
      val errorFunction: Form[PersonDetailsForm] => Future[Result] = { (formWithErrors: Form[PersonDetailsForm]) =>
        Future.successful(BadRequest(addPersonDetails(baseId, formWithErrors)))
      }

      val successFunction: PersonDetailsForm => Future[Result] = { (dataForm: PersonDetailsForm) =>
        insertSqlQueries
          .insertPersonDetails(dataForm.toPersonalDetails)
          .fold(
            InternalServerError(serviceUnavailableView("No record was inserted"))
          ) { id =>
            Redirect(controllers.routes.IndividualController.showPerson(baseId, id))
          }
      }

      val formValidationResult = PersonDetailsForm.personDetailsForm.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }

}
