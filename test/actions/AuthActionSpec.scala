package actions

import java.time.LocalDateTime

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

import models.AuthenticatedRequest
import models.Session
import models.SessionData
import models.UserData
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.OK
import play.api.http.Status.SEE_OTHER
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.InjectedController
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.session
import play.api.test.Helpers.status
import queries.SessionSqlQueries
import testUtils.BaseSpec

class AuthActionSpec extends BaseSpec {

  val mockSqlQueries: SessionSqlQueries                          = mock[SessionSqlQueries]
  def messagesControllerComponents: MessagesControllerComponents = inject[MessagesControllerComponents]

  class Harness(authAction: AuthAction) extends InjectedController {
    def onPageLoad: Action[AnyContent] = authAction { (request: AuthenticatedRequest[AnyContent]) =>
      Ok(
        s"sessionId: ${request.localSession.sessionId}, sessionData: ${request.localSession.sessionData}"
      )
    }
  }

  def fakeController: Harness = {
    val authAction =
      new AuthActionImpl(mockSqlQueries, messagesControllerComponents)(global)
    new Harness(authAction)
  }

  "A user with no existing session" in {
    when(mockSqlQueries.getSessionData(any())).thenReturn(
      Future.successful(None)
    )
    when(mockSqlQueries.putSessionData(any())).thenReturn(
      Future.successful(None)
    )

    val result = fakeController.onPageLoad(FakeRequest("GET", "/"))
    status(result) mustBe SEE_OTHER
    session(result).data.keys must contain("sessionId")
  }

  "A user with existing session" in {
    val sessionId = "123456-123456"
    val userData  = UserData(1, "name", "hashedPassword", true, true)
    when(mockSqlQueries.getSessionData(any())).thenReturn(
      Future.successful(Some(Session(sessionId, SessionData(Some(userData)), LocalDateTime.now)))
    )
    when(mockSqlQueries.sessionKeepAlive(any())).thenReturn(
      Future.successful(1)
    )

    val result = fakeController.onPageLoad(FakeRequest("GET", "/").withSession(("sessionId", sessionId)))
    status(result) mustBe OK
    contentAsString(result) must include(sessionId)
    contentAsString(result) must include(userData.toString)
    verify(mockSqlQueries, times(1)).sessionKeepAlive(any())
  }

}
