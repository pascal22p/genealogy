package services

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.data.OptionT
import cats.implicits.*
import models.queryData.EventDetailQueryData
import models.EventDetail
import models.EventType.FamilyEvent
import models.EventType.IndividualEvent
import models.EventType.UnknownEvent
import models.Place
import models.SourCitation
import models.SourCitationType.EventSourCitation
import queries.GetSqlQueries

@Singleton
class EventService @Inject() (mariadbQueries: GetSqlQueries, sourCitationService: SourCitationService)(
    implicit ec: ExecutionContext
) {
  def getIndividualEvents(personId: Int, omitSources: Boolean = false): Future[List[EventDetail]] = {
    mariadbQueries.getEvents(personId, IndividualEvent).flatMap { events =>
      events.traverse { event =>
        fillExtraData(event, omitSources)
      }
    }
  }

  def getFamilyEvents(familyId: Int, omitSources: Boolean = false): Future[List[EventDetail]] = {
    mariadbQueries.getEvents(familyId, FamilyEvent).flatMap { events =>
      events.traverse { event =>
        fillExtraData(event, omitSources)
      }
    }
  }

  def getEvent(eventId: Int): Future[Option[EventDetail]] = {
    mariadbQueries.getEvents(eventId, UnknownEvent).flatMap { events =>
      events.headOption.traverse { event =>
        fillExtraData(event)
      }
    }
  }

  def getOrphanedEvents(dbId: Int) = {
    mariadbQueries.getOrphanedEvents(dbId).map { events =>
      events.map(event => EventDetail(event, none, List.empty))
    }
  }

  private def fillExtraData(event: EventDetailQueryData, omitSources: Boolean = false): Future[EventDetail] = {
    def getSourCitations: Future[List[SourCitation]] = {
      if (omitSources) {
        Future.successful(List.empty[SourCitation])
      } else {
        sourCitationService.getSourCitations(
          event.events_details_id,
          EventSourCitation,
          event.dbId
        )
      }
    }

    for {
      place                       <- OptionT.fromOption[Future](event.place_id).flatMap(mariadbQueries.getPlace).value
      sources: List[SourCitation] <- getSourCitations
    } yield {
      EventDetail(event, place, sources)
    }
  }

}
