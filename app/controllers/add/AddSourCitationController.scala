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
import services.GenealogyDatabaseService
import views.html.add.AddSourCitation
import views.html.ServiceUnavailable

@Singleton
class AddSourCitationController @Inject() (
    authJourney: AuthJourney,
    insertSqlQueries: InsertSqlQueries,
    getSqlQueries: GetSqlQueries,
    genealogyDatabaseService: GenealogyDatabaseService,
    addSourCitationView: AddSourCitation,
    serviceUnavailableView: ServiceUnavailable,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def showForm(baseId: Int, ownerId: Int, sourCitationType: SourCitationType): Action[AnyContent] =
    authJourney.authWithAdminRight.async { implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      for {
        database <- genealogyDatabaseService.getGenealogyDatabase(baseId)
        records  <- getSqlQueries.getAllSourRecords
      } yield {
        val form = SourCitationForm.sourCitationForm
        Ok(addSourCitationView(form, database, ownerId, sourCitationType, records))
      }
    }

  def onSubmit(baseId: Int, ownerId: Int, sourCitationType: SourCitationType): Action[AnyContent] =
    authJourney.authWithAdminRight.async { implicit authenticatedRequest =>
      val errorFunction: Form[SourCitationForm] => Future[Result] = { (formWithErrors: Form[SourCitationForm]) =>
        for {
          database <- genealogyDatabaseService.getGenealogyDatabase(baseId)
          records  <- getSqlQueries.getAllSourRecords
        } yield {
          BadRequest(addSourCitationView(formWithErrors, database, ownerId, sourCitationType, records))
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
