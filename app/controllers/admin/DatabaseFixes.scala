package controllers.admin

import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthJourney
import cats.implicits.*
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import queries.GetSqlQueries
import queries.UpdateSqlQueries
import utils.CalendarConstants
import utils.GedcomDateLibrary

@Singleton
class DatabaseFixes @Inject() (
    authJourney: AuthJourney,
    getSqlQueries: GetSqlQueries,
    updateSqlQueries: UpdateSqlQueries,
    val controllerComponents: ControllerComponents
)(
    implicit ec: ExecutionContext
) extends BaseController
    with I18nSupport {

  def calculateDaysForDates: Action[AnyContent] = authJourney.authWithAdminRight.async { implicit request =>
    val result: Future[List[Option[Int]]] = getSqlQueries.getAllEvents.flatMap { events =>
      events.traverse { event =>
        val localDate = GedcomDateLibrary.extractDate(event.events_details_gedcom_date)

        localDate match {
          case Some(date) =>
            val days = ChronoUnit.DAYS.between(CalendarConstants.startOfAllTime, date)
            updateSqlQueries.updateEventNumberOfDays(event.events_details_id, Some(days)).map(Some(_))
          case None =>
            updateSqlQueries.updateEventNumberOfDays(event.events_details_id, None).map(_ => None)
        }
      }
    }

    result.map { changes =>
      val nones   = changes.filter(_.isEmpty)
      val changed = changes.filter(_.contains(1))
      val failed  = changes.filter(_.contains(0))
      Ok(s"Changed: ${changed.size}, Failed: ${failed.size}, No change: ${nones.size}")
    }
  }
}
