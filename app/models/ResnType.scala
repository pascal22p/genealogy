package models

object ResnType {

  def fromString(s: String): Option[ResnType] = {
    s match {
      case s if s == PrivacyResn.toString      => Some(PrivacyResn)
      case s if s == LockedResn.toString       => Some(LockedResn)
      case s if s == ConfidentialResn.toString => Some(ConfidentialResn)
      case _                                   => None
    }
  }

  sealed trait ResnType

  case object PrivacyResn extends ResnType {
    override def toString = "Privacy"
  }

  case object LockedResn extends ResnType {
    override def toString = "Locked"
  }

  case object ConfidentialResn extends ResnType {
    override def toString = "Confidential"
  }
}
