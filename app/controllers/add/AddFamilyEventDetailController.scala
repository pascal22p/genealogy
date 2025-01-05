package controllers.add

import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthAction
import actions.AuthJourney
import models.forms.EventDetailForm
import models.AuthenticatedRequest
import models.Person
import play.api.data.Form
import play.api.i18n.*
import play.api.mvc.*
import play.api.Logging
import queries.InsertSqlQueries
import services.PersonService
import services.SessionService
import views.html.add.AddEventDetail
import views.html.ServiceUnavailable
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.accountmenu.PersonalDetails
import queries.GetSqlQueries
import models.EventType.FamilyEvent

@Singleton
class AddFamilyEventDetailController @Inject() (
    authJourney: AuthJourney,
    personService: PersonService,
    sessionService: SessionService,
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

  def showForm(familyId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
    val filled = EventDetailForm(authenticatedRequest.localSession.sessionData.dbId, None, None, "", "", "", "", "")
    val form = EventDetailForm.eventDetailForm.fill(filled)
    getSqlQueries.getAllPlaces.map { allPlace =>
      Ok(addEventDetailsView(form, familyId, allPlace, FamilyEvent))
    }
  }

  def onSubmit(familyId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async { implicit authenticatedRequest =>
    val errorFunction: Form[EventDetailForm] => Future[Result] = { (formWithErrors: Form[EventDetailForm]) =>
      getSqlQueries.getAllPlaces.map { allPlace =>
        BadRequest(addEventDetailsView(formWithErrors, familyId, allPlace, FamilyEvent))
      }
    }

    val successFunction: EventDetailForm => Future[Result] = { (dataForm: EventDetailForm) =>
      println("Add family event")
      insertSqlQueries.insertEventDetail(dataForm.toEventDetailOnlyQueryData, familyId, FamilyEvent).fold(
        InternalServerError(serviceUnavailableView("No record was inserted"))
      ){ id =>
        Redirect(controllers.routes.EventController.showEvent(id))
      }
    }

    val formValidationResult = EventDetailForm.eventDetailForm.bindFromRequest()
    formValidationResult.fold(errorFunction, successFunction)
  }

}
