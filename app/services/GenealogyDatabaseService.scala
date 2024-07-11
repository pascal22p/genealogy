package services

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.*
import cats.implicits.*
import models.Events
import models.GenealogyDatabase
import models.Person
import queries.MariadbQueries

@Singleton
class GenealogyDatabaseService @Inject() (mariadbQueries: MariadbQueries, personDetailsService: PersonDetailsService)(
    implicit ec: ExecutionContext
) {
  def getGenealogyDatabases: Future[List[GenealogyDatabase]] = mariadbQueries.getGenealogyDatabases

  def getSurnamesList(id: Int): Future[List[String]] = mariadbQueries.getSurnamesList(id)

  def getFirstnamesList(id: Int, name: String): Future[List[Person]] = {
    mariadbQueries.getFirstnamesList(id, name).flatMap { personList =>
      personList.traverse { person =>
        personDetailsService.getIndividualEvents(person.id).map { events =>
          Person(person, Events(events), List.empty, List.empty)
        }
      }
    }
  }
}
