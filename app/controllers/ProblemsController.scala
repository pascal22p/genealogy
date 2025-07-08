package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import actions.AuthJourney
import cats.implicits.*
import config.AppConfig
import models.AuthenticatedRequest
import models.EventType.UnknownEvent
import models.Events
import models.MediaType.UnknownMedia
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import queries.GetSqlQueries
import services.EventService
import services.FamilyService
import services.PersonService
import views.html.ProblemsView

@Singleton
class ProblemsController @Inject() (
    authJourney: AuthJourney,
    getSqlQueries: GetSqlQueries,
    personService: PersonService,
    familyService: FamilyService,
    eventService: EventService,
    problemsView: ProblemsView,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext,
    appConfig: AppConfig
) extends BaseController
    with I18nSupport {

  def onload(dbId: Int): Action[AnyContent] = authJourney.authWithAdminRight.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
      for {
        orphanedMedias      <- getSqlQueries.getMedias(None, UnknownMedia, dbId)
        orphanedIndividuals <- getSqlQueries
          .getOrphanedIndividuals(dbId)
          .flatMap(_.traverse(individual => personService.getPerson(individual.id)))
          .map(_.flatten)
        orphanedFamilies <- getSqlQueries
          .getOrphanedFamilies(dbId)
          .flatMap(_.traverse(family => familyService.getFamilyDetails(family.id).value))
          .map(_.flatten)
        orphanCitations <- getSqlQueries.getOrphanedSourCitations(dbId)
        orphanedEvents  <- eventService.getOrphanedEvents(dbId)
      } yield {
        Ok(
          problemsView(
            dbId,
            orphanedMedias,
            orphanedIndividuals,
            orphanedFamilies,
            orphanCitations,
            Events(orphanedEvents, None, UnknownEvent)
          )
        )
      }
  }
}
