package controllers.link

import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthJourney
import anorm.NamedParameter
import cats.data.OptionT
import models.forms.LinkForm
import models.AuthenticatedRequest
import models.MediaType.UnknownMedia
import play.api.data.Form
import play.api.i18n.*
import play.api.mvc.*
import queries.GetSqlQueries
import queries.InsertSqlQueries
import services.GenealogyDatabaseService
import views.html.link.LinkSourCitationToMedia

@Singleton
class LinkSourCitationToMediaController @Inject() (
    authJourney: AuthJourney,
    insertSqlQueries: InsertSqlQueries,
    getSqlQueries: GetSqlQueries,
    genealogyDatabaseService: GenealogyDatabaseService,
    linkSourCitationToMediaView: LinkSourCitationToMedia,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def showForm(dbId: Int, sourCitationId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      val form = LinkForm.linkForm
      (for {
        database  <- OptionT(genealogyDatabaseService.getGenealogyDatabase(dbId))
        allMedias <- OptionT.liftF(getSqlQueries.getMedias(None, UnknownMedia, dbId))
      } yield {
        Ok(linkSourCitationToMediaView(Some(database), sourCitationId, form, allMedias))
      }).getOrElse(NotFound(s"Database $dbId not found"))
  }

  def onSubmit(dbId: Int, sourCitationId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest =>
      val errorFunction: Form[LinkForm] => Future[Result] = { (formWithErrors: Form[LinkForm]) =>
        (for {
          database  <- OptionT(genealogyDatabaseService.getGenealogyDatabase(dbId))
          allMedias <- OptionT.liftF(getSqlQueries.getMedias(None, UnknownMedia, dbId))
        } yield {
          BadRequest(linkSourCitationToMediaView(Some(database), sourCitationId, formWithErrors, allMedias))
        }).getOrElse(NotFound(s"Database $dbId not found"))
      }

      val successFunction: LinkForm => Future[Result] = { (dataForm: LinkForm) =>
        insertSqlQueries
          .linkTable(
            "rel_sour_citations_multimedia",
            List(NamedParameter("sour_citations_id", sourCitationId), NamedParameter("media_id", dataForm.linkId))
          )
          .map { _ =>
            Redirect(controllers.routes.SourCitationController.showSourCitation(dbId, sourCitationId))
          }
      }

      val formValidationResult = LinkForm.linkForm.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }

}
