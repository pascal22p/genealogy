package controllers.add

import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthAction
import actions.AuthJourney
import models.forms.DatabaseForm
import models.AuthenticatedRequest
import models.Person
import play.api.data.Form
import play.api.i18n.*
import play.api.mvc.*
import play.api.Logging
import queries.InsertSqlQueries
import services.PersonService
import services.SessionService
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.accountmenu.PersonalDetails
import views.html.add.AddDatabase
import views.html.ServiceUnavailable

@Singleton
class AddDatabaseController @Inject() (
    authJourney: AuthJourney,
    personService: PersonService,
    sessionService: SessionService,
    insertSqlQueries: InsertSqlQueries,
    addDatabaseView: AddDatabase,
    serviceUnavailableView: ServiceUnavailable,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport
    with Logging {

  def showForm: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      val form = DatabaseForm.databaseForm
      Future.successful(Ok(addDatabaseView(form)))
  }

  def onSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async { implicit authenticatedRequest =>
    val errorFunction: Form[DatabaseForm] => Future[Result] = { (formWithErrors: Form[DatabaseForm]) =>
      Future.successful(BadRequest(addDatabaseView(formWithErrors)))
    }

    val successFunction: DatabaseForm => Future[Result] = { (dataForm: DatabaseForm) =>
      insertSqlQueries
        .insertDatabase(dataForm.toGenealogyDatabase)
        .fold(
          InternalServerError(serviceUnavailableView("No record was inserted"))
        ) { id =>
          Redirect(controllers.routes.HomeController.onload())
        }
    }

    val formValidationResult = DatabaseForm.databaseForm.bindFromRequest()
    formValidationResult.fold(errorFunction, successFunction)
  }

}
