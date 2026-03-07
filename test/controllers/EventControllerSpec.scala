package controllers

import java.time.LocalDateTime

import scala.concurrent.Future

import actions.AuthAction
import models.*
import models.ResnType.PrivacyResn
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.scalatestplus.play.*
import play.api
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.*
import play.api.test.Helpers.*
import services.EventService
import services.GenealogyDatabaseService
import services.PersonService
import testUtils.BaseSpec
import testUtils.FakeAuthAction

class EventControllerSpec extends BaseSpec {
  val userData: UserData                                     = UserData(1, "username", "hashedPassword", true, true)
  val fakeAuthAction: FakeAuthAction                         = new FakeAuthAction(Session("id", SessionData(Some(userData)), LocalDateTime.now))
  val mockEventService: EventService                         = mock[EventService]
  val mockGenealogyDatabaseService: GenealogyDatabaseService = mock[GenealogyDatabaseService]
  val mockPersonService: PersonService                       = mock[PersonService]

  protected override def localGuiceApplicationBuilder(): GuiceApplicationBuilder =
    GuiceApplicationBuilder()
      .overrides(
        bind[AuthAction].toInstance(fakeAuthAction),
        bind[EventService].toInstance(mockEventService),
        bind[GenealogyDatabaseService].toInstance(mockGenealogyDatabaseService),
        bind[PersonService].toInstance(mockPersonService)
      )

  val sut: EventController = app.injector.instanceOf[EventController]

  "showEvent" must {
    "display event details" when {
      "privacy is set and see_privacy is true" in {
        when(mockEventService.getEvent(any())).thenReturn(
          Future.successful(Some(fakeEventDetail(privacyRestriction = Some(PrivacyResn), ownerId = Some(1))))
        )
        when(mockGenealogyDatabaseService.getGenealogyDatabase(any())).thenReturn(
          Future.successful(Some(GenealogyDatabase(1, "name", "description", None)))
        )
        when(mockPersonService.getPerson(any(), any(), any(), any())).thenReturn(
          Future.successful(
            Some(
              Person(
                fakePersonDetails(),
                Events(List.empty, None, EventType.UnknownEvent),
                Attributes(List.empty, None, EventType.UnknownEvent)
              )
            )
          )
        )

        val result = sut.showEvent(1, 1).apply(FakeRequest().withHeaders(("seePrivacy", "true")))
        status(result) mustBe OK
        verify(mockEventService, times(1)).getEvent(any())
        verify(mockGenealogyDatabaseService, times(1)).getGenealogyDatabase(any())
        verify(mockPersonService, times(1)).getPerson(any(), any(), any(), any())
        val html = Jsoup.parse(contentAsString(result))
        html.getElementById("event-details") must not be null
      }
    }

    "privacy is not set and see_privacy is false" in {
      when(mockEventService.getEvent(any())).thenReturn(
        Future.successful(Some(fakeEventDetail(ownerId = Some(1))))
      )
      when(mockGenealogyDatabaseService.getGenealogyDatabase(any())).thenReturn(
        Future.successful(Some(GenealogyDatabase(1, "name", "description", None)))
      )
      when(mockPersonService.getPerson(any(), any(), any(), any())).thenReturn(
        Future.successful(
          Some(
            Person(
              fakePersonDetails(),
              Events(List.empty, None, EventType.UnknownEvent),
              Attributes(List.empty, None, EventType.UnknownEvent)
            )
          )
        )
      )

      val result = sut.showEvent(1, 1).apply(FakeRequest().withHeaders(("seePrivacy", "false")))
      status(result) mustBe OK
      val html = Jsoup.parse(contentAsString(result))
      html.getElementById("event-details") must not be null
    }
  }

  "not display event details" when {
    "privacy is set and see_privacy is false" in {
      when(mockEventService.getEvent(any())).thenReturn(
        Future.successful(Some(fakeEventDetail(privacyRestriction = Some(PrivacyResn), ownerId = Some(1))))
      )
      when(mockGenealogyDatabaseService.getGenealogyDatabase(any())).thenReturn(
        Future.successful(Some(GenealogyDatabase(1, "name", "description", None)))
      )
      when(mockPersonService.getPerson(any(), any(), any(), any())).thenReturn(
        Future.successful(
          Some(
            Person(
              fakePersonDetails(),
              Events(List.empty, None, EventType.UnknownEvent),
              Attributes(List.empty, None, EventType.UnknownEvent)
            )
          )
        )
      )

      val result = sut.showEvent(1, 1).apply(FakeRequest().withHeaders(("seePrivacy", "false")))
      status(result) mustBe FORBIDDEN
    }

    "privacy is set and UserData is None" in {
      when(mockEventService.getEvent(any())).thenReturn(
        Future.successful(Some(fakeEventDetail(privacyRestriction = Some(PrivacyResn), ownerId = Some(1))))
      )
      when(mockGenealogyDatabaseService.getGenealogyDatabase(any())).thenReturn(
        Future.successful(Some(GenealogyDatabase(1, "name", "description", None)))
      )
      when(mockPersonService.getPerson(any(), any(), any(), any())).thenReturn(
        Future.successful(
          Some(
            Person(
              fakePersonDetails(),
              Events(List.empty, None, EventType.UnknownEvent),
              Attributes(List.empty, None, EventType.UnknownEvent)
            )
          )
        )
      )

      val result = sut.showEvent(1, 1).apply(FakeRequest().withHeaders(("userData", "false")))
      status(result) mustBe FORBIDDEN
    }
  }

  "returns not found" in {
    when(mockEventService.getEvent(any())).thenReturn(
      Future.successful(None)
    )
    when(mockGenealogyDatabaseService.getGenealogyDatabase(any())).thenReturn(
      Future.successful(Some(GenealogyDatabase(1, "name", "description", None)))
    )
    when(mockPersonService.getPerson(any(), any(), any(), any())).thenReturn(
      Future.successful(
        Some(
          Person(
            fakePersonDetails(),
            Events(List.empty, None, EventType.UnknownEvent),
            Attributes(List.empty, None, EventType.UnknownEvent)
          )
        )
      )
    )

    val result = sut.showEvent(1, 1).apply(FakeRequest())
    status(result) mustBe NOT_FOUND
  }
}
