package services

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.implicits.*
import models.EventDetail
import models.EventType.FamilyEvent
import models.EventType.IndividualEvent
import models.EventType.UnknownEvent
import models.UserData
import org.mindrot.jbcrypt.BCrypt
import queries.MariadbQueries

@Singleton
class EventService @Inject() (mariadbQueries: MariadbQueries)(
    implicit ec: ExecutionContext
) {
  def getIndividualEvents(personId: Int): Future[List[EventDetail]] = {
    mariadbQueries.getEvents(personId, IndividualEvent).flatMap { events =>
      events.traverse { event =>
        event.place_id.traverse(mariadbQueries.getPlace).map { place =>
          EventDetail(event, place.flatten)
        }
      }
    }
  }

  def getFamilyEvents(familyId: Int): Future[List[EventDetail]] = {
    mariadbQueries.getEvents(familyId, FamilyEvent).flatMap { events =>
      events.traverse { event =>
        event.place_id.traverse(mariadbQueries.getPlace).map { place =>
          EventDetail(event, place.flatten)
        }
      }
    }
  }

  def getEvent(eventId: Int): Future[Option[EventDetail]] = {
    mariadbQueries.getEvents(eventId, UnknownEvent).flatMap { events =>
      events.headOption.traverse { event =>
        event.place_id.traverse(mariadbQueries.getPlace).map { place =>
          EventDetail(event, place.flatten)
        }
      }
    }
  }

}
