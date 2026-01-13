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

@Singleton
class FirstnamesListService @Inject() (
    getSqlQueries: GetSqlQueries,
    appConfig: AppConfig
)(implicit ec: ExecutionContext) {
  private val PAGE_NUMBER = 6

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

    for {
      currentNames <- getSqlQueries.getFirstNamesList(dbId, name, appConfig.pageSize * PAGE_NUMBER, cursor)
      currentPosition = currentNames.headOption.map(name => (name.firstname, name.id, name.birthJd, name.deathJd))
      previousNames <- getSqlQueries.getFirstNamesList(
        dbId,
        name,
        appConfig.pageSize * (PAGE_NUMBER + 1),
        currentPosition,
        reverse = true
      )
    } yield {
      val previousCursorsRaw = previousNames // multiple pages
        .grouped(appConfig.pageSize) // group by individual page
        .map(_.lastOption.map(makeCursor)) // get last cursor for each page
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
      val previousCursors = previousCursorsRaw.takeRight(3 + (3 - nextCursorsRaw.size).max(0))
      val nextCursors     = nextCursorsRaw.take(3 + (3 - previousCursorsRaw.size).max(0))

      val (first, previous) = if (previousCursors.lengthIs == previousCursorsRaw.length) {
        (None, previousCursors)
      } else {
        (previousCursors.headOption, previousCursors.drop(1))
      }

      println(s"${nextCursorsRaw.map(_.index)} / ${nextCursors.map(_.index)}")
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
