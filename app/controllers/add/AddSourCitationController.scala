package controllers.add

import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthJourney
import models.forms.SourCitationForm
import models.AuthenticatedRequest
import models.SourCitationType.SourCitationType
import play.api.data.Form
import play.api.i18n.*
import play.api.mvc.*
import play.api.Logging
import queries.InsertSqlQueries
import views.html.add.AddSourCitation
import views.html.ServiceUnavailable

@Singleton
class AddSourCitationController @Inject() (
    authJourney: AuthJourney,
    insertSqlQueries: InsertSqlQueries,
    addSourCitationView: AddSourCitation,
    serviceUnavailableView: ServiceUnavailable,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport
    with Logging {

  def showForm(baseId: Int, ownerId: Int, sourCitationType: SourCitationType): Action[AnyContent] =
    authJourney.authWithAdminRight.async { implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      val form = SourCitationForm.sourCitationForm
      Future.successful(Ok(addSourCitationView(form, baseId, ownerId, sourCitationType)))
    }

  def onSubmit(baseId: Int, ownerId: Int, sourCitationType: SourCitationType): Action[AnyContent] =
    authJourney.authWithAdminRight.async { implicit authenticatedRequest =>
      val errorFunction: Form[SourCitationForm] => Future[Result] = { (formWithErrors: Form[SourCitationForm]) =>
        Future.successful(BadRequest(addSourCitationView(formWithErrors, baseId, ownerId, sourCitationType)))
      }

      val successFunction: SourCitationForm => Future[Result] = { (dataForm: SourCitationForm) =>
        insertSqlQueries
          .insertSourCitation(dataForm.toSourCitationQueryData(ownerId, baseId, sourCitationType))
          .fold(
            InternalServerError(serviceUnavailableView("No record was inserted"))
          ) { _ =>
            Redirect(controllers.routes.EventController.showEvent(baseId, ownerId))
          }
      }

      val formValidationResult = SourCitationForm.sourCitationForm.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
    }

}
