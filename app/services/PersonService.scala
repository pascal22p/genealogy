package services

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import models.*

@Singleton
class PersonService @Inject() (
    personDetailsService: PersonDetailsService
)(implicit ec: ExecutionContext) {

  def getPerson(id: Int): Future[Option[Person]] = {
    for {
      personDetails          <- personDetailsService.getPersonDetails(id)
      events                 <- personDetailsService.getIndividualEvents(id)
      parents                <- personDetailsService.getParents(id)
      families: List[Family] <- personDetailsService.getFamiliesAsPartner(id)
    } yield {
      personDetails.map(Person(_, Events(events), parents, families))
    }
  }

}
