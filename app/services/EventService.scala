package services

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.implicits.*
import models.queryData.EventDetailQueryData
import models.EventDetail
import models.EventType.FamilyEvent
import models.EventType.IndividualEvent
import models.EventType.UnknownEvent
import models.Place
import models.SourCitation
import models.SourCitationQueryData
import models.SourCitationType.EventSourCitation
import models.UserData
import org.mindrot.jbcrypt.BCrypt
import queries.MariadbQueries

@Singleton
class EventService @Inject() (mariadbQueries: MariadbQueries, sourCitationService: SourCitationService)(
    implicit ec: ExecutionContext
) {
  def getIndividualEvents(personId: Int): Future[List[EventDetail]] = {
    mariadbQueries.getEvents(personId, IndividualEvent).flatMap { events =>
      events.traverse { event =>
        fillExtraData(event)
      }
    }
  }

  def getFamilyEvents(familyId: Int): Future[List[EventDetail]] = {
    mariadbQueries.getEvents(familyId, FamilyEvent).flatMap { events =>
      events.traverse { event =>
        fillExtraData(event)
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

  private def fillExtraData(event: EventDetailQueryData): Future[EventDetail] = {
    for {
      place: Option[Option[Place]] <- event.place_id.traverse(mariadbQueries.getPlace)
      sources: List[SourCitation] <- sourCitationService.getSourCitations(
        event.events_details_id,
        EventSourCitation
      )
    } yield {
      EventDetail(event, place.flatten, sources)
    }
  }

}
