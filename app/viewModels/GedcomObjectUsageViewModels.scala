package viewModels

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.data.OptionT
import cats.implicits.toTraverseOps
import config.AppConfig
import models.AuthenticatedRequest
import models.GedcomObjectType
import models.GedcomObjectUsage
import play.api.i18n.Messages
import queries.GetSqlQueries
import services.EventService
import services.FamilyService
import services.PersonDetailsService

@Singleton
class GedcomObjectUsageViewModels @Inject() (
    getSqlQueries: GetSqlQueries,
    eventService: EventService,
    personDetailsService: PersonDetailsService,
    familyService: FamilyService
)(implicit ec: ExecutionContext, appConfig: AppConfig) {
  def buildViewModel(
      dbId: Int,
      usages: List[GedcomObjectUsage]
  )(
      implicit messages: Messages,
      authenticatedRequest: AuthenticatedRequest[?]
  ): Future[List[SourCitationUsageViewModel]] = {
    usages.traverse { usage =>
      val owner = usage.ownerType match {
        case GedcomObjectType.Event =>
          eventService.getEvent(usage.ownerId).map { event =>
            HtmlLinkViewModel(
              event.eventShortDescription,
              controllers.routes.EventController.showEvent(event.base, event.events_details_id).url
            )
          }

        case GedcomObjectType.Individual =>
          personDetailsService.getPersonDetails(usage.ownerId).map { person =>
            HtmlLinkViewModel(
              person.shortName,
              controllers.routes.IndividualController.showPerson(person.base, person.id).url
            )
          }

        case GedcomObjectType.Family =>
          familyService.getFamilyDetails(usage.ownerId, omitSources = true).map { family =>
            HtmlLinkViewModel(
              family.formatFamilyName,
              controllers.routes.FamilyController.showFamily(dbId, family.id).url
            )
          }

        case GedcomObjectType.Multimedia =>
          getSqlQueries.getMedia(dbId, usage.ownerId).map { media =>
            HtmlLinkViewModel(
              media.title,
              "not implemented"
            )
          }

        case GedcomObjectType.Note =>
          OptionT.none[Future, HtmlLinkViewModel]
      }

      val persons = usage.personIds.traverse { id =>
        personDetailsService.getPersonDetails(id).map { personDetails =>
          HtmlLinkViewModel(
            personDetails.shortName,
            controllers.routes.IndividualController.showPerson(personDetails.base, personDetails.id).url
          )
        }: OptionT[Future, HtmlLinkViewModel]
      }

      for {
        ownerOption <- owner.value
        personsList <- persons.value
      } yield {
        SourCitationUsageViewModel(owner = ownerOption, persons = personsList.getOrElse(Nil))
      }
    }
  }

}
