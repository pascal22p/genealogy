package models

final case class Attributes(eventsDetails: List[EventDetail], ownerId: Option[Int], ownerType: EventType)
    extends EventsOrAttributes
