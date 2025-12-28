package models.journeyCache

import java.time.LocalDateTime

import models.forms.CreateNewDatabaseForm
import models.journeyCache.UserAnswersKey.NewDatabaseDetailsQuestion
import models.AuthenticatedRequest
import models.Session
import models.SessionData
import models.UserData
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import testUtils.BaseSpec

class UserAnswersSpec extends BaseSpec {

  implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] =
    AuthenticatedRequest(
      FakeRequest(),
      Session("", SessionData(Some(UserData(0, "", "", true, true))), LocalDateTime.now),
      None
    )

  "UserAmswers.getItem" must {
    "get the item as instance of A" in {
      val record = CreateNewDatabaseForm("title", "description")

      val sut = UserAnswers(Map(NewDatabaseDetailsQuestion -> record))

      sut.getItem(NewDatabaseDetailsQuestion) mustBe record
    }
  }
}
