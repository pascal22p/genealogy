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
    override def toString = "eventSourCitation"
  }

  case object IndividualSourCitation extends SourCitationType {
    override def toString = "individualSourCitation"
  }

  case object FamilySourCitation extends SourCitationType {
    override def toString = "familySourCitation"
  }

  case object UnknownSourCitation extends SourCitationType {
    override def toString = "unknownSourCitation"
  }
}
