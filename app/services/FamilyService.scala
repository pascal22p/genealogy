package services

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.implicits.*
import models.Child
import models.EventDetail
import models.EventType.FamilyEvent
import models.Events
import models.Family
import models.Person
import queries.MariadbQueries

@Singleton
class FamilyService @Inject() (
    mariadbQueries: MariadbQueries,
    personDetailsService: PersonDetailsService,
    eventService: EventService
)(
    implicit ec: ExecutionContext
) {
  def getChildren(familyId: Int): Future[List[Child]] = {
    mariadbQueries.getChildren(familyId).flatMap { children =>
      children.traverse { (child: Child) =>
        eventService.getIndividualEvents(child.person.details.id).map { (events: List[EventDetail]) =>
          child.copy(person = child.person.copy(events = Events(events)))
        }

      }
    }
  }

  def getFamiliesAsPartner(id: Int): Future[List[Family]] = {
    mariadbQueries.getFamiliesAsPartner(id).flatMap { families =>
      families
        .traverse(family => getFamilyDetails(family.id))
        .flatMap { families =>
          families.flatten.traverse { family =>
            for {
              events   <- eventService.getFamilyEvents(family.id)
              children <- getChildren(family.id)
            } yield {
              family.copy(children = children, events = Events(events))
            }
          }
        }
    }
  }

  def getFamilyDetails(id: Int): Future[Option[Family]] = {
    mariadbQueries
      .getFamilyDetails(id)
      .flatMap(families =>
        families.traverse { family =>
          for {
            parent1 <- family.parent1.traverse(personDetailsService.getPersonDetails).map(_.flatten)
            events1 <- parent1.traverse(i => eventService.getIndividualEvents(i.id))
            parent2 <- family.parent2.traverse(personDetailsService.getPersonDetails).map(_.flatten)
            events2 <- parent2.traverse(i => eventService.getIndividualEvents(i.id))
          } yield {
            Family(
              family,
              parent1.map(Person(_, Events(events1.getOrElse(List.empty[EventDetail])), List.empty)),
              parent2.map(Person(_, Events(events2.getOrElse(List.empty[EventDetail])), List.empty))
            )
          }
        }
      )
  }
}
