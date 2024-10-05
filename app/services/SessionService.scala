package services

import javax.inject.Inject
import javax.inject.Singleton

import models.AuthenticatedRequest
import models.HistoryElement
import models.Person
import queries.SessionSqlQueries

@Singleton
class SessionService @Inject() (sqlQueries: SessionSqlQueries) {
  def insertPersonInHistory(person: Person)(implicit request: AuthenticatedRequest[?]) = {
    val currentHistory = request.localSession.sessionData.history
    val newHistory =
      List(HistoryElement(person.details.id, person.name)) ++ currentHistory.filterNot(_.personId == person.details.id)
    val newSessionData = request.localSession.sessionData.copy(history = newHistory.take(5))
    val newSession     = request.localSession.copy(sessionData = newSessionData)
    sqlQueries.updateSessionData(newSession)
  }
}
