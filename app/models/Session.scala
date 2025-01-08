package models

import java.time.LocalDateTime

import anorm.*
import anorm.SqlParser.*
import play.api.libs.json.Json

final case class Session(sessionId: String, sessionData: SessionData, timeStamp: LocalDateTime)

object Session {
  val mysqlParser: RowParser[Session] =
    (get[String]("sessionId") ~
      get[String]("sessionData") ~
      get[LocalDateTime]("timeStamp")).map {
      case id ~ sessionData ~ timeStamp =>
        Session(id, Json.parse(sessionData).as[SessionData], timeStamp)
    }
}
