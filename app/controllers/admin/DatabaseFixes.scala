package controllers.admin

import javax.inject.Singleton
import javax.inject.Inject
import actions.AuthJourney
import queries.GetSqlQueries
import queries.UpdateSqlQueries
import play.api.mvc.ControllerComponents
import scala.concurrent.ExecutionContext
import play.api.i18n.I18nSupport
import play.api.Logging
import play.api.mvc.BaseController
import utils.CalendarConstants
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale
import utils.GedcomDateLibrary
import java.time.LocalDate
import scala.util.{Try, Success, Failure}
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import scala.concurrent.Future
import cats.implicits._


@Singleton
class DatabaseFixes @Inject() (
    authJourney: AuthJourney,
    getSqlQueries: GetSqlQueries,
    updateSqlQueries: UpdateSqlQueries,
    gedcomDateLibrary: GedcomDateLibrary,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport
    with Logging {
        
  def calculateDaysForDates = authJourney.authWithAdminRight.async { implicit request =>
    val result: Future[List[Option[Int]]] = getSqlQueries.getAllEvents.flatMap { events =>
      events.traverse { event =>

        val localDate = gedcomDateLibrary.extractDate(event.events_details_gedcom_date)

        localDate match {
          case Some(date) =>
            val days = ChronoUnit.DAYS.between(CalendarConstants.startOfAllTime, date)
            updateSqlQueries.updateEventNumberofDays(event.events_details_id, Some(days)).map(Some(_))
          case None =>
            updateSqlQueries.updateEventNumberofDays(event.events_details_id, None).map(_ => None)
        }
      }
    }

    result.map { changes =>
      val nones = changes.filter(_.isEmpty)
      val changed = changes.filter(_ == Some(1))
      val failed = changes.filter(_ == Some(0))
      Ok(s"Changed: ${changed.size}, Failed: ${failed.size}, No change: ${nones.size}")
    }
  }
}
