package services

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.*
import cats.implicits.*
import models.*
import models.EventType.FamilyEvent
import models.EventType.IndividualEvent
import queries.GetSqlQueries

@Singleton
class PersonDetailsService @Inject() (
    mariadbQueries: GetSqlQueries,
    eventService: EventService
)(
    implicit ec: ExecutionContext
) {

  def getPersonDetails(id: Int): Future[Option[PersonDetails]] =
    mariadbQueries.getPersonDetails(id).map(_.headOption)

  def getParents(id: Int): Future[List[Parents]] = {
    mariadbQueries.getFamiliesFromIndividualId(id).flatMap { families =>
      families.map { familyQueryData =>
        for {
          parent1 <- familyQueryData.family.parent1.traverse(getPersonDetails).map(_.flatten)
          events1 <- parent1.traverse(i => eventService.getIndividualEvents(i.id))
          parent2 <- familyQueryData.family.parent2.traverse(getPersonDetails).map(_.flatten)
          events2 <- parent2.traverse(i => eventService.getIndividualEvents(i.id))
        } yield {
          Parents(
            familyQueryData,
            parent1.map(parent =>
              Person(
                parent,
                Events(events1.getOrElse(List.empty[EventDetail]), Some(parent.id), IndividualEvent),
                List.empty
              )
            ),
            parent2.map(parent =>
              Person(
                parent,
                Events(events2.getOrElse(List.empty[EventDetail]), Some(parent.id), IndividualEvent),
                List.empty
              )
            )
          )
        }
      }.sequence
    }
  }

}
