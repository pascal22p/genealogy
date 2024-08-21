package config

import play.api.http.Status.BAD_REQUEST
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.test.FakeRequest
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.status
import testUtils.BaseSpec

class ErrorHandlerSpec extends BaseSpec {
  val sut = new ErrorHandler

  "onClientError" must {
    "return the status as is" in {
      val result = sut.onClientError(FakeRequest(), BAD_REQUEST, "Message")

      status(result) mustBe BAD_REQUEST
    }

    "onServerError" must {
      "return an Internal server error" in {
        val result = sut.onServerError(FakeRequest(), new RuntimeException("test"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
