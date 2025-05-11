package utils

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

import scala.util.Failure
import scala.util.Success
import scala.util.Try

@Singleton
class RepublicanDate @Inject() () {

  private def frenchMonthsToGregorianYear123567(year: Int): Map[String, (String, Int, Int)] = Map(
    "VEND" -> ("SEP", 22, 1791 + year),
    "BRUM" -> ("OCT", 22, 1791 + year),
    "FRIM" -> ("NOV", 21, 1791 + year),
    "NIVO" -> ("DEC", 21, 1791 + year),
    "PLUV" -> ("JAN", 20, 1792 + year),
    "VENT" -> ("FEB", 19, 1792 + year),
    "GERM" -> ("MAR", 21, 1792 + year),
    "FLOR" -> ("APR", 20, 1792 + year),
    "PRAI" -> ("MAY", 20, 1792 + year),
    "MESS" -> ("JUN", 19, 1792 + year),
    "THER" -> ("JUL", 19, 1792 + year),
    "FRUC" -> ("AUG", 18, 1792 + year),
    "COMP" -> ("SEP", 17, 1792 + year)
  )

  private def frenchMonthsToGregorianYear4(year: Int): Map[String, (String, Int, Int)] = Map(
    "VEND" -> ("SEP", 23, 1791 + year),
    "BRUM" -> ("OCT", 23, 1791 + year),
    "FRIM" -> ("NOV", 22, 1791 + year),
    "NIVO" -> ("DEC", 22, 1791 + year),
    "PLUV" -> ("JAN", 21, 1792 + year),
    "VENT" -> ("FEB", 20, 1792 + year),
    "GERM" -> ("MAR", 21, 1792 + year),
    "FLOR" -> ("APR", 20, 1792 + year),
    "PRAI" -> ("MAY", 20, 1792 + year),
    "MESS" -> ("JUN", 19, 1792 + year),
    "THER" -> ("JUL", 19, 1792 + year),
    "FRUC" -> ("AUG", 18, 1792 + year),
    "COMP" -> ("SEP", 17, 1792 + year)
  )

  private def frenchMonthsToGregorianYear89ABDE(year: Int): Map[String, (String, Int, Int)] = Map(
    "VEND" -> ("SEP", 23, 1791 + year),
    "BRUM" -> ("OCT", 23, 1791 + year),
    "FRIM" -> ("NOV", 22, 1791 + year),
    "NIVO" -> ("DEC", 22, 1791 + year),
    "PLUV" -> ("JAN", 21, 1792 + year),
    "VENT" -> ("FEB", 20, 1792 + year),
    "GERM" -> ("MAR", 22, 1792 + year),
    "FLOR" -> ("APR", 21, 1792 + year),
    "PRAI" -> ("MAY", 21, 1792 + year),
    "MESS" -> ("JUN", 20, 1792 + year),
    "THER" -> ("JUL", 20, 1792 + year),
    "FRUC" -> ("AUG", 19, 1792 + year),
    "COMP" -> ("SEP", 17, 1792 + year)
  )

  private def frenchMonthsToGregorianYearC(year: Int): Map[String, (String, Int, Int)] = Map(
    "VEND" -> ("SEP", 24, 1791 + year),
    "BRUM" -> ("OCT", 24, 1791 + year),
    "FRIM" -> ("NOV", 23, 1791 + year),
    "NIVO" -> ("DEC", 23, 1791 + year),
    "PLUV" -> ("JAN", 22, 1792 + year),
    "VENT" -> ("FEB", 21, 1792 + year),
    "GERM" -> ("MAR", 22, 1792 + year),
    "FLOR" -> ("APR", 21, 1792 + year),
    "PRAI" -> ("MAY", 21, 1792 + year),
    "MESS" -> ("JUN", 20, 1792 + year),
    "THER" -> ("JUL", 20, 1792 + year),
    "FRUC" -> ("AUG", 19, 1792 + year),
    "COMP" -> ("SEP", 17, 1792 + year)
  )

  private val frenchMonthsToGregorianYearAll: Map[Int, Map[String, (String, Int, Int)]] = Map(
    1  -> frenchMonthsToGregorianYear123567(1),
    2  -> frenchMonthsToGregorianYear123567(2),
    3  -> frenchMonthsToGregorianYear123567(3),
    4  -> frenchMonthsToGregorianYear4(4),
    5  -> frenchMonthsToGregorianYear123567(5),
    6  -> frenchMonthsToGregorianYear123567(6),
    7  -> frenchMonthsToGregorianYear123567(7),
    8  -> frenchMonthsToGregorianYear89ABDE(8),
    9  -> frenchMonthsToGregorianYear89ABDE(9),
    10 -> frenchMonthsToGregorianYear89ABDE(10),
    11 -> frenchMonthsToGregorianYear89ABDE(11),
    12 -> frenchMonthsToGregorianYearC(12),
    13 -> frenchMonthsToGregorianYear89ABDE(13),
    14 -> frenchMonthsToGregorianYear89ABDE(14)
  )

  def romanToInt(roman: String): Int = {
    val romanNumerals = Map('I' -> 1, 'V' -> 5, 'X' -> 10)
    roman
      .foldRight((0, 0)) {
        case (c, (sum, lastSeen)) =>
          val value = romanNumerals(c)
          if (value < lastSeen) (sum - value, value)
          else (sum + value, value)
      }
      ._1
  }

  def parseRepublicanDate(date: String): Option[(Int, String, Int)] = {
    val dateRegexExact          = """([0-9]+)\s+([A-Za-z]{3,4})\s+([0-9]+)""".r
    val dateRegexExactRoman     = """([0-9]+)\s+([A-Za-z]{3,4})\s+([IVX]+)""".r
    val dateRegexMonthYear      = """([A-Za-z]{3,4})\s+([0-9]+)""".r
    val dateRegexMonthYearRoman = """([A-Za-z]{3,4})\s+([IVX]+)""".r
    val dateRegexYearRoman      = """([IVX]+)""".r
    val dateRegexYear           = """([0-9]+)""".r
    date match {
      case dateRegexExact(day, month, year)      => Some((day.toInt, month, year.toInt))
      case dateRegexExactRoman(day, month, year) => Some((day.toInt, month, romanToInt(year)))
      case dateRegexMonthYear(month, year)       => Some((1, month, year.toInt))
      case dateRegexMonthYearRoman(month, year)  => Some((1, month, romanToInt(year)))
      case dateRegexYear(year)                   => Some((1, "VEND", year.toInt))
      case dateRegexYearRoman(year)              => Some((1, "VEND", romanToInt(year)))
      case _                                     => None
    }
  }

  def fromRepublicanToGregorian(day: Int, month: String, year: Int): Option[LocalDate] = {
    val (gregorianMonth, gregorianDay, gregorianYear) = frenchMonthsToGregorianYearAll(year)(month)
    Try(
      LocalDate
        .of(gregorianYear, CalendarConstants.gregorianMonthsToInt(gregorianMonth), gregorianDay)
        .plusDays(day - 1)
    ) match {
      case Success(result) => Some(result)
      case Failure(_)      => None
    }
  }

  def parseRepublicanStringToGregorianDate(date: String): Option[LocalDate] = {
    parseRepublicanDate(date).map {
      case (day, month, year) => fromRepublicanToGregorian(day, month, year)
    }.flatten
  }
}
