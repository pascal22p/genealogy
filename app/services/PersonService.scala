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
      familyService.getFamiliesAsPartner(id, omitSources)
    }
  }

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
      personDetails.map(Person(_, Events(events), parents, families))
    }
  }

}
