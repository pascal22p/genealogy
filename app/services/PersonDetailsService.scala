package services

import models.*
import queries.MariadbQueries
import cats.*
import cats.implicits.*
import models.queryData.EventDetailQueryData

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PersonDetailsService @Inject()(mariadbQueries: MariadbQueries)(
  implicit ec: ExecutionContext
) {

  def getPersonDetails(id: Int): Future[Option[PersonDetails]] =
    mariadbQueries.getPersonDetails(id).map(_.headOption)

  def getIndividualEvents(personId: Int): Future[List[EventDetail]] = {
    mariadbQueries.getIndividualEvents(personId).flatMap { events =>
      events.traverse { event =>
        event.place_id.traverse(mariadbQueries.getPlace).map { place =>
          EventDetail(event, place.flatten)
        }
      }
    }
  }

  def getFamilyEvents(familyId: Int): Future[List[EventDetail]] = {
    mariadbQueries.getFamilyEvents(familyId).flatMap { events =>
      events.traverse { event =>
        event.place_id.traverse(mariadbQueries.getPlace).map { place =>
          EventDetail(event, place.flatten)
        }
      }
    }
  }

  def getParents(id: Int): Future[List[Parents]] = {
    mariadbQueries.getFamiliesFromIndividualId(id).flatMap { families =>
      families.map { familyQueryData =>
          for {
            parent1 <- familyQueryData.family.parent1.traverse(getPersonDetails).map(_.flatten)
            events1 <- parent1.traverse(i => getIndividualEvents(i.id))
            parent2 <- familyQueryData.family.parent2.traverse(getPersonDetails).map(_.flatten)
            events2 <- parent2.traverse(i => getIndividualEvents(i.id))
          } yield {
            Parents(
              familyQueryData,
              parent1.map(Person(_, Events(events1.getOrElse(List.empty)), List.empty)),
              parent2.map(Person(_, Events(events2.getOrElse(List.empty)), List.empty))
            )
          }
      }.sequence
    }
  }

  
  def getFamiliesAsPartner(id: Int): Future[List[Family]] = {
    mariadbQueries.getFamiliesAsPartner(id).flatMap { families =>
      families.traverse(family => getFamilyDetails(family.id)
      .map(_.get))
      .flatMap { families =>
        families.traverse { family =>
          for {
            events <- getFamilyEvents(family.id)
            children <- getChildren(family.id)
          } yield {
            family.copy(children = children, events = Events(events))
          }
        }
      }
   }
  }

  def getFamilyDetails(id: Int): Future[Option[Family]] = {
    mariadbQueries.getFamilyDetails(id).flatMap ( families =>
      families.traverse { family =>
        for {
          parent1 <- family.parent1.traverse(getPersonDetails).map(_.flatten)
          events1 <- parent1.traverse(i => getIndividualEvents(i.id))
          parent2 <- family.parent2.traverse(getPersonDetails).map(_.flatten)
          events2 <- parent2.traverse(i => getIndividualEvents(i.id))
        } yield {
         Family(
            family,
            parent1.map(Person(_, Events(events1.getOrElse(List.empty)), List.empty)),
            parent2.map(Person(_, Events(events2.getOrElse(List.empty)), List.empty))
         )
        }
      }
    )
  }

  def getChildren(familyId: Int): Future[List[Child]] = {
    mariadbQueries.getChildren(familyId).flatMap { children =>
      children.traverse { (child: Child) =>
        getIndividualEvents(child.person.details.id).map { (events: List[EventDetail]) =>
          child.copy(person = child.person.copy(events = Events(events)))
        }

      }
    }
  }

}
