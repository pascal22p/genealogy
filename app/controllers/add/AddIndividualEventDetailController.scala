package controllers.add

import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthJourney
import models.forms.EventDetailForm
import models.AuthenticatedRequest
import models.EventType.IndividualEvent
import play.api.data.Form
import play.api.i18n.*
import play.api.mvc.*
import play.api.Logging
import queries.GetSqlQueries
import queries.InsertSqlQueries
import views.html.add.AddEventDetail
import views.html.ServiceUnavailable

@Singleton
class AddIndividualEventDetailController @Inject() (
    authJourney: AuthJourney,
    insertSqlQueries: InsertSqlQueries,
    addEventDetailsView: AddEventDetail,
    serviceUnavailableView: ServiceUnavailable,
    getSqlQueries: GetSqlQueries,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport
    with Logging {

  def showForm(baseId: Int, personId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      val filled = EventDetailForm(baseId, None, None, "", "", "", "", "")
      val form   = EventDetailForm.eventDetailForm.fill(filled)
      getSqlQueries.getAllPlaces.map { allPlace =>
        Ok(addEventDetailsView(baseId, form, personId, allPlace, IndividualEvent))
      }
  }

  def onSubmit(baseId: Int, personId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest =>
      val errorFunction: Form[EventDetailForm] => Future[Result] = { (formWithErrors: Form[EventDetailForm]) =>
        getSqlQueries.getAllPlaces.map { allPlace =>
          BadRequest(addEventDetailsView(baseId, formWithErrors, personId, allPlace, IndividualEvent))
        }
      }

      val successFunction: EventDetailForm => Future[Result] = { (dataForm: EventDetailForm) =>
        insertSqlQueries
          .insertEventDetail(dataForm.toEventDetailQueryData(IndividualEvent, personId))
          .fold(
            InternalServerError(serviceUnavailableView("No record was inserted"))
          ) { id =>
            Redirect(controllers.routes.EventController.showEvent(baseId, id))
          }
      }

      val formValidationResult = EventDetailForm.eventDetailForm.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }

}
