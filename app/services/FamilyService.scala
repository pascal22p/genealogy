package services

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.implicits.*
import models.Attributes
import models.Child
import models.EventDetail
import models.EventType.FamilyEvent
import models.EventType.IndividualEvent
import models.Events
import models.Family
import models.Person
import queries.GetSqlQueries

@Singleton
class FamilyService @Inject() (
    mariadbQueries: GetSqlQueries,
    personDetailsService: PersonDetailsService,
    eventService: EventService
)(
    implicit ec: ExecutionContext
) {
  def getChildren(familyId: Int, omitSources: Boolean = false): Future[List[Child]] = {
    mariadbQueries.getChildren(familyId).flatMap { children =>
      children.traverse { (child: Child) =>
        eventService.getIndividualEvents(child.person.details.id, omitSources).map { (events: List[EventDetail]) =>
          child
            .copy(person = child.person.copy(events = Events(events, Some(child.person.details.id), IndividualEvent)))
        }
      }
    }
  }

  def getFamilyIdsFromPartnerId(id: Int): Future[List[Int]] = {
    mariadbQueries.getFamilyIdsFromPartnerId(id)
  }

  def getFamilyDetails(id: Int, omitSources: Boolean = false): Future[Option[Family]] = {
    mariadbQueries
      .getFamilyDetails(id)
      .flatMap(families =>
        families.traverse { family =>
          for {
            parent1      <- family.parent1.traverse(personDetailsService.getPersonDetails).map(_.flatten)
            events1      <- parent1.traverse(i => eventService.getIndividualEvents(i.id, omitSources))
            parent2      <- family.parent2.traverse(personDetailsService.getPersonDetails).map(_.flatten)
            events2      <- parent2.traverse(i => eventService.getIndividualEvents(i.id, omitSources))
            familyEvents <- eventService.getFamilyEvents(id)
            children     <- getChildren(family.id, omitSources)
          } yield {
            Family(
              family,
              parent1.map(
                Person(
                  _,
                  Events(events1.getOrElse(List.empty[EventDetail]), Some(family.id), FamilyEvent),
                  Attributes(List.empty, Some(family.id), FamilyEvent),
                  List.empty
                )
              ),
              parent2.map(
                Person(
                  _,
                  Events(events2.getOrElse(List.empty[EventDetail]), Some(family.id), FamilyEvent),
                  Attributes(List.empty, Some(family.id), FamilyEvent),
                  List.empty
                )
              ),
              children,
              familyEvents
            )
          }
        }
      )
  }
}
