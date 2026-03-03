package controllers

import javax.inject.*

import scala.concurrent.ExecutionContext

import actions.AuthAction
import models.AuthenticatedRequest
import models.SourCitationType.UnknownSourCitation
import play.api.i18n.*
import play.api.mvc.*
import services.GenealogyDatabaseService
import services.SourCitationService
import views.html.SourCitationPage

@Singleton
class SourCitationController @Inject() (
    authAction: AuthAction,
    sourCitationService: SourCitationService,
    sourCitationPage: SourCitationPage,
    genealogyDatabaseService: GenealogyDatabaseService,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def showSourCitation(dbId: Int, id: Int): Action[AnyContent] = authAction.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      for {
        database      <- genealogyDatabaseService.getGenealogyDatabase(dbId)
        sourCitations <- sourCitationService.getSourCitations(id, UnknownSourCitation, dbId)
      } yield {
        sourCitations.headOption.fold(NotFound("Source citation not found")) { sourCitation =>
          Ok(sourCitationPage(database, sourCitation))
        }
      }
  }
}
