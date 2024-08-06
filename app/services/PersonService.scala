package services

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import models.*

@Singleton
class PersonService @Inject() (
    personDetailsService: PersonDetailsService,
    familyService: FamilyService,
    eventService: EventService
)(implicit ec: ExecutionContext) {

  def getPerson(id: Int): Future[Option[Person]] = {
    for {
      personDetails          <- personDetailsService.getPersonDetails(id)
      events                 <- eventService.getIndividualEvents(id)
      parents                <- personDetailsService.getParents(id)
      families: List[Family] <- familyService.getFamiliesAsPartner(id)
    } yield {
      personDetails.map(Person(_, Events(events), parents, families))
    }
  }

}
