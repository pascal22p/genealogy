package controllers.add

import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthJourney
import models.forms.NewDatabaseForm
import models.AuthenticatedRequest
import play.api.data.Form
import play.api.i18n.*
import play.api.mvc.*
import queries.InsertSqlQueries
import views.html.add.AddDatabase
import views.html.ServiceUnavailable

@Singleton
class AddDatabaseController @Inject() (
    authJourney: AuthJourney,
    insertSqlQueries: InsertSqlQueries,
    addDatabaseView: AddDatabase,
    serviceUnavailableView: ServiceUnavailable,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  private val onSubmitDestination: Call = controllers.add.routes.AddDatabaseController.onSubmit

  def showForm: Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      val form = NewDatabaseForm.databaseForm
      Future.successful(Ok(addDatabaseView(form, onSubmitDestination)))
  }

  def onSubmit: Action[AnyContent] = authJourney.authWithAdminRight.async { implicit authenticatedRequest =>
    val errorFunction: Form[NewDatabaseForm] => Future[Result] = { (formWithErrors: Form[NewDatabaseForm]) =>
      Future.successful(BadRequest(addDatabaseView(formWithErrors, onSubmitDestination)))
    }

    val successFunction: NewDatabaseForm => Future[Result] = { (dataForm: NewDatabaseForm) =>
      insertSqlQueries
        .insertDatabase(dataForm.toGenealogyDatabase)
        .fold(
          InternalServerError(serviceUnavailableView("No record was inserted"))
        ) { _ =>
          Redirect(controllers.routes.HomeController.onload())
        }
    }

    val formValidationResult = NewDatabaseForm.databaseForm.bindFromRequest()
    formValidationResult.fold(errorFunction, successFunction)
  }

}
