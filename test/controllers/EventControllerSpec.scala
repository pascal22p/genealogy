package controllers

import java.time.LocalDateTime

import scala.concurrent.Future

import actions.AuthAction
import models.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.*
import play.api
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.*
import play.api.test.Helpers.*
import services.EventService
import testUtils.BaseSpec
import testUtils.FakeAuthAction

class EventControllerSpec extends BaseSpec {
  val userData: UserData             = UserData(1, "username", "hashedPassword", true, true)
  val fakeAuthAction: FakeAuthAction = new FakeAuthAction(Session("id", SessionData(Some(userData)), LocalDateTime.now))
  val mockEventService: EventService = mock[EventService]

  protected override def localGuiceApplicationBuilder(): GuiceApplicationBuilder =
    GuiceApplicationBuilder()
      .overrides(
        bind[AuthAction].toInstance(fakeAuthAction),
        bind[EventService].toInstance(mockEventService)
      )

  val sut: EventController = app.injector.instanceOf[EventController]

  "showEvent" must {
    "display event details" when {
      "privacy is set and see_privacy is true" in {
        when(mockEventService.getEvent(any())).thenReturn(
          Future.successful(Some(fakeEventDetail(privacyRestriction = Some("privacy"))))
        )

        val result = sut.showEvent(1, 1).apply(FakeRequest().withHeaders(("seePrivacy", "true")))
        status(result) mustBe OK
        contentAsString(result) must include("Orphan event")
      }
    }

    "privacy is not set and see_privacy is false" in {
      when(mockEventService.getEvent(any())).thenReturn(
        Future.successful(Some(fakeEventDetail()))
      )

      val result = sut.showEvent(1, 1).apply(FakeRequest().withHeaders(("seePrivacy", "false")))
      status(result) mustBe OK
      contentAsString(result) must include("Orphan event")
    }
  }

  "not display event details" when {
    "privacy is set and see_privacy is false" in {
      when(mockEventService.getEvent(any())).thenReturn(
        Future.successful(Some(fakeEventDetail(privacyRestriction = Some("privacy"))))
      )

      val result = sut.showEvent(1, 1).apply(FakeRequest().withHeaders(("seePrivacy", "false")))
      status(result) mustBe FORBIDDEN
    }

    "privacy is set and UserData is None" in {
      when(mockEventService.getEvent(any())).thenReturn(
        Future.successful(Some(fakeEventDetail(privacyRestriction = Some("privacy"))))
      )

      val result = sut.showEvent(1, 1).apply(FakeRequest().withHeaders(("userData", "false")))
      status(result) mustBe FORBIDDEN
    }
  }

  "returns not found" in {
    when(mockEventService.getEvent(any())).thenReturn(
      Future.successful(None)
    )

    val result = sut.showEvent(1, 1).apply(FakeRequest())
    status(result) mustBe NOT_FOUND
  }
}
