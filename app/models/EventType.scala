package models

enum EventType derives CanEqual {
  case IndividualEvent
  case IndividualAttribute
  case FamilyEvent
  case UnknownEvent
}

object EventType {
  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  def fromString(s: String): EventType =
    EventType.values.find(_.toString == s).getOrElse(UnknownEvent)
}
