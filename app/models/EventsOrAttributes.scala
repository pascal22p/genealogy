package models

trait EventsOrAttributes {
  val eventsDetails: List[EventDetail]
  val ownerId: Option[Int]
  val ownerType: EventType.EventType
}
