package queries

import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.Future

import anorm.*
import models.DatabaseExecutionContext
import play.api.db.Database

@Singleton
final class JourneyCacheQueries @Inject() (db: Database, databaseExecutionContext: DatabaseExecutionContext) {

  def getUserAnswers(sessionId: String): Future[Option[(String, String, LocalDateTime)]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT sessionId, data, lastUpdated
            |FROM genea_user_answers
            |WHERE sessionId = {sessionId}""".stripMargin)
        .on("sessionId" -> sessionId)
        .as(
          (SqlParser.str("sessionId") ~ SqlParser.str("data") ~ SqlParser
            .get[LocalDateTime]("lastUpdated")(using anorm.Column.columnToLocalDateTime)).singleOpt
        )
        .map { case a ~ b ~ c => (a, b, c) }
    }
  }(using databaseExecutionContext)

  def deleteUserAnswers(sessionId: String): Future[Int] = Future {
    db.withConnection { implicit conn =>
      SQL("""DELETE FROM genea_user_answers
            |WHERE sessionId = {sessionId}""".stripMargin)
        .on("sessionId" -> sessionId)
        .executeUpdate()
    }
  }(using databaseExecutionContext)

  def upsertUserAnswers(sessionId: String, data: String): Future[Int] = Future {
    db.withConnection { implicit conn =>
      SQL("""REPLACE INTO genea_user_answers (sessionId, data, lastUpdated)
            |VALUES ({sessionId}, {data}, {lastUpdated})""".stripMargin)
        .on(
          "sessionId"   -> sessionId,
          "data"        -> data,
          "lastUpdated" -> LocalDateTime.now
        )
        .executeUpdate()
    }
  }(using databaseExecutionContext)

  def updateLastUpdated(sessionId: String): Future[Int] = Future {
    db.withConnection { implicit conn =>
      SQL("""UPDATE genea_user_answers
            |SET lastUpdated = {lastUpdated}
            |WHERE sessionId = {sessionId}""".stripMargin)
        .on(
          "sessionId"   -> sessionId,
          "lastUpdated" -> LocalDateTime.now
        )
        .executeUpdate()
    }
  }(using databaseExecutionContext)

}
