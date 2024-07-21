package testUtils

import actions.AuthAction
import models.{AuthenticatedRequest, Session, SessionData}
import play.api.mvc.{AnyContent, BodyParser, Request, Result}

import scala.concurrent.{ExecutionContext, Future}

class FakeAuthAction (localSession: Session) extends AuthAction {
  def parser: BodyParser[AnyContent] = play.api.test.Helpers.stubBodyParser()

  def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    val seePrivacy = request.headers.get("seePrivacy").exists(_.toBoolean)
    val userData = request.headers.get("userData").forall(_.toBoolean)
    val newSession = (userData, seePrivacy) match {
      case (false, _) =>     Session("1", localSession.sessionData.copy(userData = None))
      case (true, privacy) => Session("1", SessionData(1, localSession.sessionData.userData.map(_.copy(seePrivacy = privacy))))
    }
    block(AuthenticatedRequest(request, newSession))
  }

  protected def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global
}
