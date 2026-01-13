package utils

import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.time.LocalDate
import java.util.Locale

import scala.util.Try

import config.AppConfig
import models.AuthenticatedRequest
import play.api.i18n.Messages

object GedcomDateLibrary {

  def birthAndDeathDate(
      birthDate: Option[String],
      deathDate: Option[String],
      shortMonth: Boolean = false,
      yearOnly: Boolean = false
  )(
      implicit messages: Messages,
      authenticatedRequest: AuthenticatedRequest[?],
      appConfig: AppConfig
  ): String = {
    (birthDate.filter(_.nonEmpty), deathDate.filter(_.nonEmpty)) match {
      case (None, None)               => ""
      case (Some(date), None)         => formatDate(date, shortMonth, yearOnly).fold("")(d => s"°$d")
      case (None, Some(date))         => formatDate(date, shortMonth, yearOnly).fold("")(d => s"†$d")
      case (Some(date1), Some(date2)) =>
        (formatDate(date1, shortMonth, yearOnly), formatDate(date2, shortMonth, yearOnly)) match {
          case (None, None)               => ""
          case (Some(date), None)         => s"°$date"
          case (None, Some(date))         => s"†$date"
          case (Some(date1), Some(date2)) => s"°$date1 – †$date2"
        }
    }
  }

  def formatDate(events_details_gedcom_date: String, shortMonth: Boolean = false, yearOnly: Boolean = false)(
      implicit messages: Messages,
      authenticatedRequest: AuthenticatedRequest[?],
      appConfig: AppConfig
  ): Option[String] = {
    if (yearOnly) {
      formatDateYear(events_details_gedcom_date)
    } else {
      Some(formatDateLong(events_details_gedcom_date, shortMonth))
    }
  }

  def formatDateYear(events_details_gedcom_date: String) = {
    val dateRegex = ".*([0-9]{4}).*".r
    dateRegex.findFirstMatchIn(events_details_gedcom_date).map(_.group(1))
  }

  def formatDateLong(events_details_gedcom_date: String, shortMonth: Boolean = false)(
      implicit messages: Messages,
      authenticatedRequest: AuthenticatedRequest[?],
      appConfig: AppConfig
  ): String = {
    val dateRegex               = ".*([0-9]{4}).*".r
    val isAllowedToSee: Boolean = authenticatedRequest.localSession.sessionData.userData.fold(false)(_.seePrivacy)
    val shortMonthMessage       =
      if (shortMonth) {
        ".short"
      } else {
        ""
      }

    val maxYear = dateRegex.findAllMatchIn(events_details_gedcom_date).toList.map(_.group(1).toInt).maxOption
    maxYear match {
      case Some(year) if year > 1900 && !isAllowedToSee => appConfig.redactedMask
      case _                                            =>
        val dateWithoutCal = CalendarConstants.calendarTypes.foldLeft(events_details_gedcom_date) {
          case (formattedDate, replace) =>
            replace._1.replaceAllIn(formattedDate, messages(replace._2))
        }
        CalendarConstants.allKeywords
          .foldLeft(dateWithoutCal) {
            case (formattedDate, replace) =>
              replace._1.replaceAllIn(formattedDate, messages(replace._2 + shortMonthMessage))
          }
          .trim
    }
  }

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
      case republicanDateRegex(_) => RepublicanDate.parseRepublicanStringToGregorianDate(gedcomDate)
      case _                      => {
        val localDate = Try(LocalDate.parse(trimmedGedcomDate, dateFormatter))
          .map(Some(_))
          .getOrElse(None)

        localDate
      }
    }
  }

  def dayCountToGregorianDate(days: Int): LocalDate = {
    CalendarConstants.startOfAllTime.plusDays(days)
  }
}
