package models

sealed trait Calendar {
  val gedcom: String
}

object Calendar {
  def fromString(s: String): Option[Calendar] = {
    s.match {
      case UnknownCalendar.gedcom   => Some(UnknownCalendar)
      case GregorianCalendar.gedcom => Some(GregorianCalendar)
      case JulianCalendar.gedcom    => Some(JulianCalendar)
      case FrenchCalendar.gedcom    => Some(FrenchCalendar)
      case HebrewCalendar.gedcom    => Some(HebrewCalendar)
      case RomanCalendar.gedcom     => Some(RomanCalendar)
      case _                        => None
    }
  }
}

case object UnknownCalendar extends Calendar {
  val gedcom = "@#DUNKNOWN@"
}

case object GregorianCalendar extends Calendar {
  val gedcom = "@#DGREGORIAN@"
}

case object JulianCalendar extends Calendar {
  val gedcom = "@#DJULIAN@"
}

case object FrenchCalendar extends Calendar {
  val gedcom = "@#DFRENCH@"
}

case object HebrewCalendar extends Calendar {
  val gedcom = "@#DHEBREW@"
}

case object RomanCalendar extends Calendar {
  val gedcom = "@#DROMAN@"
}
