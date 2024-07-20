package controllers

import scala.concurrent.Future

import actions.AuthAction
import actions.AuthActionImpl
import models.Events
import models.Person
import models.Session
import models.SessionData
import models.UserData
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.*
import play.api
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.*
import play.api.test.Helpers.*
import services.PersonService
import testUtils.BaseSpec
import testUtils.FakeAuthAction

class individualControllerSpec extends BaseSpec {
  val userData: UserData               = UserData(1, "username", "hashedPassword", true, true)
  val fakeAuthAction: FakeAuthAction   = new FakeAuthAction(Session("id", SessionData(1, Some(userData))))
  val mockPersonService: PersonService = mock[PersonService]

  protected override def localGuiceApplicationBuilder(): GuiceApplicationBuilder =
    GuiceApplicationBuilder()
      .overrides(
        bind[AuthAction].toInstance(fakeAuthAction),
        bind[PersonService].toInstance(mockPersonService)
      )

  val sut: IndividualController = app.injector.instanceOf[IndividualController]

  "showPerson" must {
    "display person details" when {
      "privacy is set and see_privacy is true" in {
        when(mockPersonService.getPerson(any())).thenReturn(
          Future.successful(Some(Person(fakePersonDetails(privacyRestriction = Some("privacy")), Events(List.empty))))
        )

        val result = sut.showPerson(1).apply(FakeRequest().withHeaders(("seePrivacy", "true")))
        status(result) mustBe OK
        contentAsString(result) must include("Firstname Surname")
      }
    }

    "privacy is not set and see_privacy is false" in {
      when(mockPersonService.getPerson(any())).thenReturn(
        Future.successful(Some(Person(fakePersonDetails(privacyRestriction = None), Events(List.empty))))
      )

      val result = sut.showPerson(1).apply(FakeRequest().withHeaders(("seePrivacy", "false")))
      status(result) mustBe OK
      contentAsString(result) must include("Firstname Surname")
    }
  }

  "not display person details" when {
    "privacy is set and see_privacy is false" in {
      when(mockPersonService.getPerson(any())).thenReturn(
        Future.successful(Some(Person(fakePersonDetails(privacyRestriction = Some("privacy")), Events(List.empty))))
      )

      val result = sut.showPerson(1).apply(FakeRequest().withHeaders(("seePrivacy", "false")))
      status(result) mustBe FORBIDDEN
    }

    "privacy is set and UserData is None" in {
      when(mockPersonService.getPerson(any())).thenReturn(
        Future.successful(Some(Person(fakePersonDetails(privacyRestriction = Some("privacy")), Events(List.empty))))
      )

      val result = sut.showPerson(1).apply(FakeRequest().withHeaders(("userData", "false")))
      status(result) mustBe FORBIDDEN
    }
  }

  "returns not found" in {
    when(mockPersonService.getPerson(any())).thenReturn(
      Future.successful(None)
    )

    val result = sut.showPerson(1).apply(FakeRequest())
    status(result) mustBe NOT_FOUND
  }
}
