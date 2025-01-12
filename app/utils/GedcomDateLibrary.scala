package utils

import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

import scala.util.Try

@Singleton
class GedcomDateLibrary @Inject() (
    republicanDate: RepublicanDate
) {

  def extractDate(gedcomDate: String): Option[LocalDate] = {
    val dateFormatter: DateTimeFormatter = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .appendOptional(DateTimeFormatter.ofPattern("d MMM u"))
      .appendOptional(DateTimeFormatter.ofPattern("MMM u"))
      .appendOptional(DateTimeFormatter.ofPattern("u"))
      .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
      .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
      .toFormatter()
      .withLocale(Locale.US)

    val republicanDateRegex =
      """.*\b(VEND|BRUM|FRIM|NIVO|PLUV|VENT|GERM|FLOR|PRAI|MESS|THER|FRUC|COMP|I|II|III|IV|V|VI|VII|VIII|IX|X|XI|XII|XIII|XIV|XV)\b.*""".r

    val removeModifiers = CalendarConstants.keywordsPeriod.map { (key, _) =>
      key -> ""
    } ++ Map("@#[A-Z ]+@".r -> "")

    val trimmedGedcomDate = removeModifiers
      .foldLeft(gedcomDate) {
        case (formattedDate, replace) =>
          replace._1.replaceAllIn(formattedDate, replace._2)
      }
      .trim

    trimmedGedcomDate match {
      case republicanDateRegex(_) => republicanDate.parseRepublicanStringToGregorianDate(gedcomDate)
      case _ => {
        val localDate = Try(LocalDate.parse(trimmedGedcomDate, dateFormatter))
          .map(Some(_))
          .getOrElse(None)

        if localDate.isEmpty then println(s"Failed to parse date: `$trimmedGedcomDate` ($gedcomDate)")

        localDate
      }
    }
  }

  def dayCountToGregorianDate(days: Int): LocalDate = {
    CalendarConstants.startOfAllTime.plusDays(days)
  }
}
