package services

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.*
import cats.implicits.*
import models.AuthenticatedRequest
import models.Events
import models.GenealogyDatabase
import models.Person
import queries.MariadbQueries

@Singleton
class GenealogyDatabaseService @Inject() (mariadbQueries: MariadbQueries, eventService: EventService)(
    implicit ec: ExecutionContext
) {
  def getGenealogyDatabases: Future[List[GenealogyDatabase]] = mariadbQueries.getGenealogyDatabases

  def getSurnamesList(id: Int)(implicit authenticatedRequest: AuthenticatedRequest[?]): Future[List[(String, Int)]] =
    mariadbQueries.getSurnamesList(id)

  def getFirstnamesList(id: Int, name: String)(
      implicit authenticatedRequest: AuthenticatedRequest[?]
  ): Future[List[Person]] = {
    mariadbQueries.getFirstnamesList(id, name).flatMap { personList =>
      personList.traverse { person =>
        eventService.getIndividualEvents(person.id).map { events =>
          Person(person, Events(events), List.empty, List.empty)
        }
      }
    }
  }
}
