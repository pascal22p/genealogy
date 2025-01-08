package testUtils

import java.time.LocalDateTime

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import actions.AuthAction
import models.AuthenticatedRequest
import models.Session
import models.SessionData
import play.api.mvc.AnyContent
import play.api.mvc.BodyParser
import play.api.mvc.Request
import play.api.mvc.Result

class FakeAuthAction(localSession: Session) extends AuthAction {
  def parser: BodyParser[AnyContent] = play.api.test.Helpers.stubBodyParser()

  def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    val seePrivacy = request.headers.get("seePrivacy").exists(_.toBoolean)
    val userData   = request.headers.get("userData").forall(_.toBoolean)
    val newSession = (userData, seePrivacy) match {
      case (false, _) => Session("1", localSession.sessionData.copy(userData = None), LocalDateTime.now)
      case (true, privacy) =>
        Session(
          "1",
          SessionData(localSession.sessionData.userData.map(_.copy(seePrivacy = privacy))),
          LocalDateTime.now
        )
    }
    block(AuthenticatedRequest(request, newSession))
  }

  protected def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global
}
