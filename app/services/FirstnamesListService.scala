package services

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import config.AppConfig
import models.queryData.FirstnameWithBirthDeath
import models.AuthenticatedRequest
import models.Cursor
import models.FirstnamesListPagination
import play.api.i18n.Messages
import queries.GetSqlQueries
import utils.GedcomDateLibrary
import cats.implicits.*

@Singleton
class FirstnamesListService @Inject() (
    getSqlQueries: GetSqlQueries,
    appConfig: AppConfig
)(implicit ec: ExecutionContext) {
  // number of pages to look ahead/behind plus the current page
  // plus an extra page to know if there are more records or not after/before this set of pages
  // should be an even number.
  private val PAGE_NUMBER = 8
  private val sidePage    = (PAGE_NUMBER - 1 - 1) / 2

  def getFirstNamesListWithAnchors(
      dbId: Int,
      name: String,
      cursor: Option[(String, Int, Int, Int)] = None
  )(implicit authenticatedRequest: AuthenticatedRequest[?], messages: Messages): Future[FirstnamesListPagination] = {
    def makeCursor(name: FirstnameWithBirthDeath): Cursor = {
      val dates =
        GedcomDateLibrary.birthAndDeathDate(name.birth, name.death, yearOnly = true)(
          using implicitly,
          implicitly,
          appConfig
        )
      if (dates.isEmpty) {
        Cursor(name.firstname, name.id, name.birthJd, name.deathJd, s"${name.firstname}")
      } else {
        Cursor(name.firstname, name.id, name.birthJd, name.deathJd, s"${name.firstname} ($dates)")
      }
    }

    // if cursor is missing, it is the first page so no previous pages
    val maybePreviousNames = cursor
      .traverse { _ =>
        getSqlQueries.getFirstNamesList(
          dbId,
          name,
          appConfig.pageSize * (PAGE_NUMBER + 1),
          cursor,
          reverse = true
        )
      }
      .map(_.getOrElse(Seq.empty))

    for {
      currentNames  <- getSqlQueries.getFirstNamesList(dbId, name, appConfig.pageSize * PAGE_NUMBER, cursor)
      previousNames <- maybePreviousNames
    } yield {
      val previousCursorsRaw = previousNames // multiple pages
        .grouped(appConfig.pageSize) // group by individual page
        .map(_.lastOption.map(makeCursor)) // get last cursor for each page (the order is reversed)
        .toSeq
        .flatten
        .reverse

      val nextCursorsRaw = currentNames // multiple pages
        .grouped(appConfig.pageSize) // group by individual page
        .map(_.headOption.map(makeCursor)) // get first cursor for each page
        .toSeq
        .flatten
        .drop(1) // first element is current page

      val currentCursor   = currentNames.headOption.map(makeCursor)
      val previousCursors = previousCursorsRaw.takeRight(sidePage + (sidePage - nextCursorsRaw.size).max(0))
      val nextCursors     = nextCursorsRaw.take(sidePage + (sidePage - previousCursorsRaw.size).max(0))

      val (first, previous) = if (previousCursors.lengthIs == previousCursorsRaw.length) {
        (None, previousCursors)
      } else {
        (previousCursors.headOption, previousCursors.drop(1))
      }

      val (last, next) = if (nextCursors.lengthIs < nextCursorsRaw.length) {
        (nextCursors.takeRight(1).headOption, nextCursors.dropRight(1))
      } else {
        (None, nextCursors)
      }

      FirstnamesListPagination(
        currentNames.take(appConfig.pageSize),
        currentCursor,
        previous,
        next,
        first,
        last
      )
    }
  }
}
