package models

object EventType {

  def fromString(s: String): EventType = {
    s match {
      case s if s == IndividualEvent.toString => IndividualEvent
      case s if s == FamilyEvent.toString     => FamilyEvent
      case _                                  => UnknownEvent
    }
  }

  sealed trait EventType

  case object IndividualEvent extends EventType {
    override def toString = "individualType"
  }

  case object FamilyEvent extends EventType {
    override def toString = "familyType"
  }

  case object UnknownEvent extends EventType {
    override def toString = "unknownType"
  }
}
