package controllers.link

import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthJourney
import models.forms.LinkForm
import models.AuthenticatedRequest
import models.MediaType.UnknownMedia
import play.api.data.Form
import play.api.i18n.*
import play.api.mvc.*
import play.api.Logging
import queries.GetSqlQueries
import queries.InsertSqlQueries
import views.html.link.LinkSourCitationToMedia
import views.html.ServiceUnavailable

@Singleton
class LinkSourCitationToMediaController @Inject() (
    authJourney: AuthJourney,
    insertSqlQueries: InsertSqlQueries,
    getSqlQueries: GetSqlQueries,
    linkSourCitationToMediaView: LinkSourCitationToMedia,
    serviceUnavailableView: ServiceUnavailable,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport
    with Logging {

  def showForm(dbId: Int, sourCitationId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      val form = LinkForm.linkForm
      getSqlQueries.getMedias(None, UnknownMedia, dbId).map { allMedias =>
        Ok(linkSourCitationToMediaView(dbId, sourCitationId, form, allMedias))
      }
  }

  def onSubmit(dbId: Int, sourCitationId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit authenticatedRequest =>
      val errorFunction: Form[LinkForm] => Future[Result] = { (formWithErrors: Form[LinkForm]) =>
        getSqlQueries.getMedias(None, UnknownMedia, dbId).map { allMedias =>
          BadRequest(linkSourCitationToMediaView(dbId, sourCitationId, formWithErrors, allMedias))
        }
      }

      val successFunction: LinkForm => Future[Result] = { (dataForm: LinkForm) =>
        insertSqlQueries
          .linkTable(
            "rel_sour_citations_multimedia",
            Map("sour_citations_id" -> sourCitationId, "media_id" -> dataForm.linkId)
          )
          .map {
            case false => InternalServerError(serviceUnavailableView("No link was inserted"))
            case true  => Redirect(controllers.routes.SourCitationController.showSourCitation(dbId, sourCitationId))
          }
      }

      val formValidationResult = LinkForm.linkForm.bindFromRequest()
      formValidationResult.fold(errorFunction, successFunction)
  }

}
