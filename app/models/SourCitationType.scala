package models

object SourCitationType {

  def fromString(s: String): SourCitationType = {
    s match {
      case s if s == EventSourCitation.toString      => EventSourCitation
      case s if s == IndividualSourCitation.toString => IndividualSourCitation
      case s if s == FamilySourCitation.toString     => FamilySourCitation
      case _                                         => UnknownSourCitation
    }
  }

  sealed trait SourCitationType

  case object EventSourCitation extends SourCitationType {
    override def toString = "event-source"
  }

  case object IndividualSourCitation extends SourCitationType {
    override def toString = "individual-source"
  }

  case object FamilySourCitation extends SourCitationType {
    override def toString = "family-source"
  }

  case object UnknownSourCitation extends SourCitationType {
    override def toString = "unknown-source"
  }
}
