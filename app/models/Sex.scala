package models

sealed trait Sex {
  val gedcom: String
}

object Sex {
  def fromString(s: String): Sex = {
    s.match {
      case MaleSex.gedcom => MaleSex
      case FemaleSex.gedcom => FemaleSex
      case UnknownSex.gedcom => UnknownSex
      case _ => UnknownSex
    }
  }
}

case object MaleSex extends Sex {
  override def toString: String = "sex.male"
  override val gedcom = "M"
}

case object FemaleSex extends Sex {
  override def toString: String = "sex.female"
  override val gedcom = "F"
}

case object UnknownSex extends Sex {
  override def toString: String = "sex.unknown"
  override val gedcom = "U"
}


