package models

object MediaType {

  def fromString(s: String): MediaType = {
    s match {
      case s if s == EventMedia.toString        => EventMedia
      case s if s == IndividualMedia.toString   => IndividualMedia
      case s if s == FamilyMedia.toString       => FamilyMedia
      case s if s == SourCitationMedia.toString => SourCitationMedia
      case _                                    => UnknownMedia
    }
  }

  sealed trait MediaType

  case object EventMedia extends MediaType {
    override def toString = "EventMedia"
  }

  case object IndividualMedia extends MediaType {
    override def toString = "IndividualMedia"
  }

  case object FamilyMedia extends MediaType {
    override def toString = "FamilyMedia"
  }

  case object SourCitationMedia extends MediaType {
    override def toString = "SourCitationMedia"
  }

  case object UnknownMedia extends MediaType {
    override def toString = "UnknownMedia"
  }
}
