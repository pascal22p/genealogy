package controllers.admin

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import actions.AuthJourney
import cats.implicits.*
import config.AppConfig
import models.AuthenticatedRequest
import cats.data.OptionT
import models.EventType.UnknownEvent
import models.Events
import models.MediaType.UnknownMedia
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import services.GenealogyDatabaseService
import queries.GetSqlQueries
import services.EventService
import services.FamilyService
import services.PersonService
import views.html.admin.ProblemsView

@Singleton
class ProblemsController @Inject() (
    authJourney: AuthJourney,
    getSqlQueries: GetSqlQueries,
    personService: PersonService,
    familyService: FamilyService,
    eventService: EventService,
    problemsView: ProblemsView,
    genealogyDatabaseService: GenealogyDatabaseService,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext,
    appConfig: AppConfig
) extends BaseController
    with I18nSupport {

  def onload(dbId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      (for {
        database            <- OptionT(genealogyDatabaseService.getGenealogyDatabase(dbId))
        orphanedMedias      <- OptionT.liftF(getSqlQueries.getMedias(None, UnknownMedia, dbId))
        orphanedIndividuals <- OptionT.liftF(
          getSqlQueries
            .getOrphanedIndividuals(dbId)
            .flatMap(_.traverse(individual => personService.getPerson(individual.id)))
            .map(_.flatten)
        )
        orphanedFamilies <- OptionT.liftF(
          getSqlQueries
            .getOrphanedFamilies(dbId)
            .flatMap(_.traverse(family => familyService.getFamilyDetails(family.id).value))
            .map(_.flatten)
        )
        orphanCitations <- OptionT.liftF(getSqlQueries.getOrphanedSourCitations(dbId))
        orphanedEvents  <- OptionT.liftF(eventService.getOrphanedEvents(dbId))
        emptyEvents     <- OptionT.liftF(eventService.getEmptyEvents(dbId))
      } yield {
        Ok(
          problemsView(
            Some(database),
            orphanedMedias,
            orphanedIndividuals,
            orphanedFamilies,
            orphanCitations,
            Events(orphanedEvents, None, UnknownEvent),
            Events(emptyEvents, None, UnknownEvent)
          )
        )
      }).getOrElse(NotFound(s"Genealogy database $dbId not found"))
  }
}
