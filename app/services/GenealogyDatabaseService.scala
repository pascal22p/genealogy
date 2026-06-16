package services

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.*
import cats.implicits.*
import models.Attributes
import models.AuthenticatedRequest
import models.EventType.IndividualEvent
import models.Events
import models.GenealogyDatabase
import models.Person
import models.SurnameElement
import queries.GetSqlQueries
import utils.GedcomDateLibrary
import io.opentelemetry.instrumentation.annotations.WithSpan

@Singleton
class GenealogyDatabaseService @Inject() (
    mariadbQueries: GetSqlQueries,
    eventService: EventService
)(
    implicit ec: ExecutionContext
) {
  @WithSpan
  def getGenealogyDatabases: Future[List[GenealogyDatabase]] = mariadbQueries.getGenealogyDatabases

  @WithSpan
  def getGenealogyDatabase(dbId: Int): Future[Option[GenealogyDatabase]] =
    mariadbQueries.getGenealogyDatabases.map(_.find(_.id == dbId))

  @WithSpan
  def getSurnamesList(id: Int)(
      implicit authenticatedRequest: AuthenticatedRequest[?]
  ): Future[List[SurnameElement]] =
    mariadbQueries.getSurnamesList(id).map { listOfNames =>
      listOfNames.map {
        case (name, count, minJdCount, maxJdCount) =>
          SurnameElement(
            name,
            count,
            minJdCount.map(GedcomDateLibrary.dayCountToGregorianDate(_).getYear()),
            maxJdCount.map(GedcomDateLibrary.dayCountToGregorianDate(_).getYear())
          )
      }
    }

  @WithSpan
  def getFirstnamesList(id: Int, name: String)(
      implicit authenticatedRequest: AuthenticatedRequest[?]
  ): Future[List[Person]] = {
    mariadbQueries.getAllPersonDetails(id, Some(name)).flatMap { personList =>
      personList.traverse { person =>
        eventService.getIndividualEvents(person.id).map { events =>
          Person(
            person,
            Events(events, Some(person.id), IndividualEvent),
            Attributes(List.empty, Some(person.id), IndividualEvent),
            List.empty,
            List.empty
          )
        }
      }
    }
  }
}
