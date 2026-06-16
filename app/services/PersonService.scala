package services

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.implicits.*
import models.*
import models.EventType.IndividualEvent
import io.opentelemetry.instrumentation.annotations.WithSpan

@Singleton
class PersonService @Inject() (
    personDetailsService: PersonDetailsService,
    familyService: FamilyService,
    eventService: EventService
)(implicit ec: ExecutionContext) {

  private def getParents(id: Int, omitParents: Boolean): Future[List[Parents]] = {
    if (omitParents) {
      Future.successful(List.empty[Parents])
    } else {
      personDetailsService.getParents(id)
    }
  }

  private def getFamilies(id: Int, omitSources: Boolean, omitFamilies: Boolean): Future[List[Family]] = {
    if (omitFamilies) {
      Future.successful(List.empty[Family])
    } else {
      familyService
        .getFamilyIdsFromPartnerId(id)
        .flatMap { (families: List[Int]) =>
          families.traverse { id =>
            familyService.getFamilyDetails(id, omitSources).value
          }
        }
        .map(_.flatten)
    }
  }

  @WithSpan
  def getPerson(
      id: Int,
      omitSources: Boolean = false,
      omitFamilies: Boolean = false,
      omitParents: Boolean = false
  ): Future[Option[Person]] = {
    for {
      personDetails          <- personDetailsService.getPersonDetails(id)
      events                 <- eventService.getIndividualEvents(id, omitSources)
      parents                <- getParents(id, omitParents)
      families: List[Family] <- getFamilies(id, omitSources, omitFamilies)
    } yield {
      personDetails.map(person =>
        Person(
          person,
          Events(events, Some(person.id), IndividualEvent),
          Attributes(List.empty, Some(person.id), IndividualEvent),
          parents,
          families
        )
      )
    }
  }

  @WithSpan
  def getPersonDetails(id: Int): Future[Option[PersonDetails]] =
    personDetailsService.getPersonDetails(id)

  @WithSpan
  def getLatestPersons(
      dbId: Int,
      maxNumber: Int
  ): Future[Seq[Person]] = {
    personDetailsService.getLatestPersonDetails(dbId, maxNumber).flatMap { personDetails =>
      personDetails.traverse { person =>
        eventService.getIndividualEvents(person.id, true).map { events =>
          Person(
            person,
            Events(events, Some(person.id), IndividualEvent),
            Attributes(List.empty, Some(person.id), IndividualEvent)
          )
        }
      }
    }
  }

  @WithSpan
  def searchPersons(
      dbId: Int,
      words: Seq[String]
  ): Future[Seq[Person]] = {
    personDetailsService.searchPersonDetails(dbId, words).flatMap { personDetails =>
      personDetails.traverse { person =>
        eventService.getIndividualEvents(person.id, true).map { events =>
          Person(
            person,
            Events(events, Some(person.id), IndividualEvent),
            Attributes(List.empty, Some(person.id), IndividualEvent)
          )
        }
      }
    }
  }

}
