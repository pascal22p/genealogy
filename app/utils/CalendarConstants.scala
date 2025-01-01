package utils

import java.time.LocalDate

import scala.util.matching.Regex

object CalendarConstants {

  val gregorianMonths: Map[Regex, String] = Map(
    "\\bJAN\\b".r -> "months.gregorian.jan",
    "\\bFEB\\b".r -> "months.gregorian.feb",
    "\\bMAR\\b".r -> "months.gregorian.mar",
    "\\bAPR\\b".r -> "months.gregorian.apr",
    "\\bMAY\\b".r -> "months.gregorian.may",
    "\\bJUN\\b".r -> "months.gregorian.jun",
    "\\bJUL\\b".r -> "months.gregorian.jul",
    "\\bAUG\\b".r -> "months.gregorian.aug",
    "\\bSEP\\b".r -> "months.gregorian.sep",
    "\\bOCT\\b".r -> "months.gregorian.oct",
    "\\bNOV\\b".r -> "months.gregorian.nov",
    "\\bDEC\\b".r -> "months.gregorian.dec"
  )

  val frenchMonths: Map[Regex, String] = Map(
    "\\bVEND\\b".r -> "months.french.vend",
    "\\bBRUM\\b".r -> "months.french.brum",
    "\\bFRIM\\b".r -> "months.french.frim",
    "\\bNIVO\\b".r -> "months.french.nivo",
    "\\bPLUV\\b".r -> "months.french.pluv",
    "\\bVENT\\b".r -> "months.french.vent",
    "\\bGERM\\b".r -> "months.french.germ",
    "\\bFLOR\\b".r -> "months.french.flor",
    "\\bPRAI\\b".r -> "months.french.prai",
    "\\bMESS\\b".r -> "months.french.mess",
    "\\bTHER\\b".r -> "months.french.ther",
    "\\bFRUC\\b".r -> "months.french.fruc",
    "\\bCOMP\\b".r -> "months.french.comp",
  )

  val keywordsPeriod: Map[Regex, String] = Map(
    "\\bBET\\b".r  -> "date.period.bet",
    "\\bAND\\b".r  -> "date.period.and",
    "\\bBEF\\b".r  -> "date.period.bef",
    "\\bAFT\\b".r  -> "date.period.aft",
    "\\bFROM\\b".r -> "date.period.from",
    "\\bTO\\b".r   -> "date.period.to",
    "\\bABT\\b".r  -> "date.period.abt",
    "\\bCAL\\b".r  -> "date.period.cal",
    "\\bEST\\b".r  -> "date.period.est"
  )

  val calendarTypes: Map[Regex, String] = Map(
    "@#[A-Z ]+@".r -> ""
  )

  val allKeywords: Map[Regex, String] = gregorianMonths ++ frenchMonths ++ keywordsPeriod ++ calendarTypes

  val gregorianMonthsToInt = Map(
    "JAN" -> 1,
    "FEB" -> 2,
    "MAR" -> 3,
    "APR" -> 4,
    "MAY" -> 5,
    "JUN" -> 6,
    "JUL" -> 7,
    "AUG" -> 8,
    "SEP" -> 9,
    "OCT" -> 10,
    "NOV" -> 11,
    "DEC" -> 12
  )

  val startOfAllTime = LocalDate.of(0, 1, 1)
}
