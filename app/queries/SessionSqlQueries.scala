package queries

import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.Future

import anorm.*
import anorm.SqlParser.*
import models.*
import play.api.db.Database
import play.api.libs.json.Json

@Singleton
final class SessionSqlQueries @Inject() (db: Database, databaseExecutionContext: DatabaseExecutionContext) {

  def getSessionData(sessionId: String): Future[Option[Session]] = Future {
    db.withConnection { implicit conn =>
      SQL("""SELECT *
            |FROM genea_sessions
            |WHERE sessionId = {id}
            |AND timeStamp > CURRENT_TIMESTAMP - INTERVAL '15' MINUTE""".stripMargin)
        .on("id" -> sessionId)
        .as(Session.mysqlParser.singleOpt)
    }
  }(using databaseExecutionContext)

  def putSessionData(session: Session): Future[Option[String]] = Future {
    db.withConnection { implicit conn =>
      SQL("""INSERT INTO genea_sessions (sessionId, sessionData, timeStamp)
            |VALUES ({id}, {data}, {timeStamp})
            |""".stripMargin)
        .on(
          "id"        -> session.sessionId,
          "data"      -> Json.toJson(session.sessionData).toString,
          "timeStamp" -> LocalDateTime.now
        )
        .executeInsert(str(1).singleOpt)
    }
  }(using databaseExecutionContext)

  def updateSessionData(session: Session): Future[Int] = Future {
    db.withConnection { implicit conn =>
      SQL("""UPDATE genea_sessions
            |SET sessionData = {data}
            |WHERE sessionId = {id}
            |""".stripMargin)
        .on("id" -> session.sessionId, "data" -> Json.toJson(session.sessionData).toString)
        .executeUpdate()
    }
  }(using databaseExecutionContext)

  def removeSessionData(session: Session): Future[Int] = Future {
    db.withConnection { implicit conn =>
      SQL("""DELETE FROM genea_sessions
            |WHERE sessionId = {id}
            |""".stripMargin)
        .on("id" -> session.sessionId)
        .executeUpdate()
    }
  }(using databaseExecutionContext)

  def sessionKeepAlive(sessionId: String): Future[Int] = Future {
    db.withConnection { implicit conn =>
      SQL("""UPDATE genea_sessions
            |SET timeStamp = CURRENT_TIMESTAMP
            |WHERE sessionId = {id}""".stripMargin)
        .on("id" -> sessionId)
        .executeUpdate()
    }
  }(using databaseExecutionContext)
}
