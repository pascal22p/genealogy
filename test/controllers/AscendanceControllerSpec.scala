package controllers

import java.time.Instant
import java.time.LocalDateTime

import scala.concurrent.Future

import actions.AuthAction
import models.*
import models.EventType.FamilyEvent
import models.EventType.IndividualEvent
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.*
import play.api
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.*
import play.api.test.Helpers.*
import services.AscendanceService
import testUtils.BaseSpec
import testUtils.FakeAuthAction

class AscendanceControllerSpec extends BaseSpec {
  val userData: UserData                            = UserData(1, "username", "hashedPassword", true, true)
  val fakeAuthAction: FakeAuthAction                = new FakeAuthAction(Session("id", SessionData(Some(userData)), LocalDateTime.now))
  lazy val mockAscendanceService: AscendanceService = mock[AscendanceService]

  protected override def localGuiceApplicationBuilder(): GuiceApplicationBuilder =
    GuiceApplicationBuilder()
      .overrides(
        bind[AuthAction].toInstance(fakeAuthAction),
        bind[AscendanceService].toInstance(mockAscendanceService)
      )

  val sut: AscendanceController = app.injector.instanceOf[AscendanceController]

  "showAscendant" must {
    "display list of ascendants" in {
      val timeStamp = Instant.now
      val personD: Person =
        Person(
          fakePersonDetails(firstname = "Firstname4", id = 4, timestamp = timeStamp),
          Events(List.empty, Some(4), IndividualEvent)
        )
      val personE: Person =
        Person(
          fakePersonDetails(firstname = "Firstname5", id = 5, timestamp = timeStamp),
          Events(List.empty, Some(5), IndividualEvent)
        )

      val familyC: Family =
        Family(3, None, Some(personE), timeStamp, None, "", List.empty, Events(List.empty, Some(3), FamilyEvent))
      val parentsC: Parents = Parents(familyC, "", "", None)
      val personC: Person =
        Person(
          fakePersonDetails(firstname = "Firstname3", id = 3, timestamp = timeStamp),
          Events(List.empty, Some(3), IndividualEvent),
          parents = List(parentsC)
        )

      val familyB: Family = Family(
        2,
        Some(personC),
        Some(personD),
        timeStamp,
        None,
        "",
        List.empty,
        Events(List.empty, Some(2), FamilyEvent)
      )
      val parentsB: Parents = Parents(familyB, "", "", None)
      val personB: Person =
        Person(
          fakePersonDetails(firstname = "Firstname2", id = 2, timestamp = timeStamp),
          Events(List.empty, Some(2), IndividualEvent),
          parents = List(parentsB)
        )

      val familyA: Family =
        Family(1, Some(personB), None, timeStamp, None, "", List.empty, Events(List.empty, Some(1), FamilyEvent))
      val parentsA: Parents = Parents(familyA, "", "", None)
      val personA: Person =
        Person(
          fakePersonDetails(firstname = "Firstname1", id = 1, timestamp = timeStamp),
          Events(List.empty, Some(1), IndividualEvent),
          parents = List(parentsA)
        )

      when(mockAscendanceService.getAscendant(any(), any())).thenReturn(
        Future.successful(Some(personA))
      )

      val result      = sut.showAscendant(1, 1).apply(FakeRequest())
      val html        = Jsoup.parse(contentAsString(result))
      val generation0 = html.getElementById("generation-0").html()
      val generation1 = html.getElementById("generation-1").html()
      val generation2 = html.getElementById("generation-2").html()
      val generation3 = html.getElementById("generation-3").html()
      status(result) mustBe OK
      generation0.toString must include("Firstname1 Surname")
      generation1.toString must include("Firstname2 Surname")
      generation2.toString must include("Firstname3 Surname")
      generation2.toString must include("Firstname4 Surname")
      generation3.toString must include("Firstname5 Surname")
    }

    "remove duplicates" in {
      val personD: Person =
        Person(fakePersonDetails(firstname = "Firstname4", id = 4), Events(List.empty, Some(4), IndividualEvent))
      val personE: Person =
        Person(fakePersonDetails(firstname = "Firstname5", id = 5), Events(List.empty, Some(5), IndividualEvent))

      val familyC: Family =
        Family(4, None, Some(personE), Instant.now, None, "", List.empty, Events(List.empty, Some(4), FamilyEvent))
      val parentsC: Parents = Parents(familyC, "", "", None)
      val personC: Person =
        Person(
          fakePersonDetails(firstname = "Firstname3", id = 3),
          Events(List.empty, Some(3), IndividualEvent),
          parents = List(parentsC)
        )

      val familyB: Family = Family(
        3,
        Some(personC),
        Some(personD),
        Instant.now,
        None,
        "",
        List.empty,
        Events(List.empty, Some(3), FamilyEvent)
      )
      val parentsB: Parents = Parents(familyB, "", "", None)
      val personB: Person =
        Person(
          fakePersonDetails(firstname = "Firstname2", id = 2),
          Events(List.empty, Some(2), IndividualEvent),
          parents = List(parentsB)
        )

      val personBB: Person =
        Person(
          fakePersonDetails(firstname = "Firstname22", id = 22),
          Events(List.empty, Some(22), IndividualEvent),
          parents = List(parentsB)
        )

      val familyA: Family = Family(
        1,
        Some(personB),
        Some(personBB),
        Instant.now,
        None,
        "",
        List.empty,
        Events(List.empty, Some(1), FamilyEvent)
      )
      val parentsA: Parents = Parents(familyA, "", "", None)
      val personA: Person =
        Person(
          fakePersonDetails(firstname = "Firstname1", id = 1),
          Events(List.empty, Some(1), IndividualEvent),
          parents = List(parentsA)
        )

      when(mockAscendanceService.getAscendant(any(), any())).thenReturn(
        Future.successful(Some(personA))
      )

      val result      = sut.showAscendant(1, 1).apply(FakeRequest())
      val html        = Jsoup.parse(contentAsString(result))
      val generation0 = html.getElementById("generation-0").html()
      val generation1 = html.getElementById("generation-1").html()
      val generation2 = html.getElementById("generation-2").html()
      val generation3 = html.getElementById("generation-3").html()
      status(result) mustBe OK
      generation0.toString must include("Firstname1 Surname")
      generation1.toString must include("Firstname2 Surname")
      generation1.toString must include("Firstname22 Surname")
      generation2.toString must include("Firstname3 Surname")
      generation2.toString must include("Firstname4 Surname")
      generation2.toString.split("Firstname4 Surname").length mustBe 2
      generation3.toString must include("Firstname5 Surname")
      generation3.toString.split("Firstname5 Surname").length mustBe 2
    }
  }
}
