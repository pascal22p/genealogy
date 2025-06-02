package actions

import java.time.LocalDateTime

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

import filters.SessionFilter
import models.AuthenticatedRequest
import models.Session
import models.SessionData
import models.UserData
import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import play.api.http.Status.OK
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.InjectedController
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.session
import play.api.test.Helpers.status
import queries.GetSqlQueries
import queries.SessionSqlQueries
import testUtils.BaseSpec

class AuthActionSpec extends BaseSpec {

  val mockSqlQueries: SessionSqlQueries                          = mock[SessionSqlQueries]
  val mockGetSqlQueries: GetSqlQueries                           = mock[GetSqlQueries]
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
      new AuthActionImpl(mockSqlQueries, mockGetSqlQueries, messagesControllerComponents)(using global)
    new Harness(authAction)
  }

  "A user with no existing session" in {
    when(mockSqlQueries.getSessionData(any())).thenReturn(
      Future.successful(None)
    )
    when(mockSqlQueries.putSessionData(any())).thenReturn(
      Future.successful(None)
    )

    implicit val mat: Materializer = app.materializer
    val filter                     = new SessionFilter()(using mat, global)
    val filteredAction             = filter(fakeController.onPageLoad)
    val result                     = filteredAction(FakeRequest("GET", "/"))

    status(result) mustBe OK
    val uuidV4Regex = """^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"""
    (session(result).get("sessionId").get must include).regex(uuidV4Regex)
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
