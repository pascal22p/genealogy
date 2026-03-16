package controllers

import javax.inject.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthAction
import cats.data.EitherT
import cats.implicits.*
import models.AuthenticatedRequest
import models.EventType
import models.ResnType.PrivacyResn
import play.api.i18n.*
import play.api.mvc.*
import services.EventService
import services.FamilyService
import services.GenealogyDatabaseService
import services.PersonService
import views.html.Event

@Singleton
class EventController @Inject() (
    authAction: AuthAction,
    genealogyDatabaseService: GenealogyDatabaseService,
    eventService: EventService,
    personService: PersonService,
    familyService: FamilyService,
    eventView: Event,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def showEvent(baseId: Int, id: Int): Action[AnyContent] = authAction.async {
    implicit authenticatedRequest: AuthenticatedRequest[AnyContent] =>
      (for {
        database <- EitherT.fromOptionF(
          genealogyDatabaseService.getGenealogyDatabase(baseId),
          NotFound("database not found")
        )
        event  <- EitherT.fromOptionF(eventService.getEvent(id), NotFound("event not found"))
        person <-
          if (event.eventType == EventType.IndividualEvent || event.eventType == EventType.IndividualAttribute) {
            EitherT.liftF(
              event.ownerId.flatTraverse(personId => personService.getPersonDetails(personId))
            )
          } else { EitherT.rightT[Future, Result](None) }
        family <-
          if (event.eventType == EventType.FamilyEvent) {
            EitherT.liftF(event.ownerId.flatTraverse(familyId => familyService.getFamilyDetails(familyId).value))
          } else { EitherT.rightT[Future, Result](None) }
      } yield {
        val isAllowedToSee = authenticatedRequest.localSession.sessionData.userData.fold(false)(_.seePrivacy)

        val owner = (person, family) match {
          case (None, None)         => ("Nothing", None)
          case (Some(person), None) =>
            (
              s"${person.shortName}",
              Some(controllers.routes.IndividualController.showPerson(baseId, person.id).url)
            )
          case (None, Some(family)) =>
            (
              s"Family ${family.formatFamilyName}",
              Some(controllers.routes.FamilyController.showFamily(baseId, family.id).url)
            )
          case (Some(_), Some(_)) =>
            throw new IllegalStateException("An event cannot be linked to both a person and a family")
        }

        if (!event.privacyRestriction.contains(PrivacyResn) || isAllowedToSee) {
          Ok(eventView(event, owner, Some(database)))
        } else {
          Forbidden("Not allowed")
        }
      }).merge
  }
}
