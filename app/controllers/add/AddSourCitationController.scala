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
import queries.GetSqlQueries
import queries.InsertSqlQueries
import views.html.add.AddSourCitation
import views.html.ServiceUnavailable

@Singleton
class AddSourCitationController @Inject() (
    authJourney: AuthJourney,
    insertSqlQueries: InsertSqlQueries,
    getSqlQueries: GetSqlQueries,
    addSourCitationView: AddSourCitation,
    serviceUnavailableView: ServiceUnavailable,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def showForm(baseId: Int, ownerId: Int, sourCitationType: SourCitationType): Action[AnyContent] =
    authJourney.authWithAdminRight.async { implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      getSqlQueries.getAllSourRecords.map { records =>
        val form = SourCitationForm.sourCitationForm
        Ok(addSourCitationView(form, baseId, ownerId, sourCitationType, records))
      }
    }

  def onSubmit(baseId: Int, ownerId: Int, sourCitationType: SourCitationType): Action[AnyContent] =
    authJourney.authWithAdminRight.async { implicit authenticatedRequest =>
      val errorFunction: Form[SourCitationForm] => Future[Result] = { (formWithErrors: Form[SourCitationForm]) =>
        getSqlQueries.getAllSourRecords.map { records =>
          BadRequest(addSourCitationView(formWithErrors, baseId, ownerId, sourCitationType, records))
        }
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
