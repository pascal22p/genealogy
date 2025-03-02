package models

import java.util.Locale

object ResnType {

  def fromString(s: String): Option[ResnType] = {
    val locale = Locale.forLanguageTag("en-us")
    s match {
      case s if s.toLowerCase(locale) == PrivacyResn.toString.toLowerCase(locale)      => Some(PrivacyResn)
      case s if s.toLowerCase(locale) == LockedResn.toString.toLowerCase(locale)       => Some(LockedResn)
      case s if s.toLowerCase(locale) == ConfidentialResn.toString.toLowerCase(locale) => Some(ConfidentialResn)
      case _                                                                           => None
    }
  }

  def fromInt(value: Int): Option[ResnType] =
    List(PrivacyResn, LockedResn, ConfidentialResn).find(_.value == value)

  sealed trait ResnType {
    def toString: String
    def value: Int
  }

  case object PrivacyResn extends ResnType {
    override def toString = "Privacy"
    override def value    = 2
  }

  case object LockedResn extends ResnType {
    override def toString = "Locked"
    override def value    = 1
  }

  case object ConfidentialResn extends ResnType {
    override def toString = "Confidential"
    override def value    = 3
  }
}
