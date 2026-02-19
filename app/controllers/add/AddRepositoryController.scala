package controllers.add

import javax.inject.*

import scala.concurrent.Future

import actions.AuthJourney
import controllers.Execution.trampoline
import models.forms.RepositoryForm
import models.AuthenticatedRequest
import play.api.data.Form
import play.api.i18n.*
import play.api.mvc.*
import queries.GetSqlQueries
import queries.InsertSqlQueries
import services.GenealogyDatabaseService
import views.html.add.AddRepositoryView
import views.html.ServiceUnavailable

@Singleton
class AddRepositoryController @Inject() (
    authJourney: AuthJourney,
    genealogyDatabaseService: GenealogyDatabaseService,
    getSqlQueries: GetSqlQueries,
    insertSqlQueries: InsertSqlQueries,
    addRepositoryView: AddRepositoryView,
    serviceUnavailableView: ServiceUnavailable,
    val controllerComponents: ControllerComponents
) extends BaseController
    with I18nSupport {

  def showForm(dbId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      for {
        maybeDb   <- genealogyDatabaseService.getGenealogyDatabases.map(_.find(_.id == dbId))
        addresses <- getSqlQueries.getAddresses(dbId)
      } yield {
        maybeDb.fold(NotFound(s"Database $dbId not found")) { db =>
          val form = RepositoryForm.repositoryForm
          Ok(addRepositoryView(form, db, addresses))
        }
      }
  }

  def onSubmit(dbId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async { implicit authenticatedRequest =>
    val errorFunction: Form[RepositoryForm] => Future[Result] = { (formWithErrors: Form[RepositoryForm]) =>
      for {
        maybeDb   <- genealogyDatabaseService.getGenealogyDatabases.map(_.find(_.id == dbId))
        addresses <- getSqlQueries.getAddresses(dbId)
      } yield {
        maybeDb.fold(NotFound(s"Database $dbId not found")) { db =>
          Ok(addRepositoryView(formWithErrors, db, addresses))
        }
      }
    }

    val successFunction: RepositoryForm => Future[Result] = { (form: RepositoryForm) =>
      insertSqlQueries
        .insertRepository(form.toRepositoryQueryData(dbId))
        .fold(
          InternalServerError(serviceUnavailableView("No record was inserted"))
        )(_ => Redirect(controllers.add.routes.AddController.index(dbId)))
    }

    val formValidationResult = RepositoryForm.repositoryForm.bindFromRequest()
    formValidationResult.fold(errorFunction, successFunction)
  }
}
