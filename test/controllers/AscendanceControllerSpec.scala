package controllers

import java.time.Instant

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
import services.AscendanceService
import testUtils.BaseSpec
import testUtils.FakeAuthAction

class AscendanceControllerSpec extends BaseSpec {
  val userData: UserData                            = UserData(1, "username", "hashedPassword", true, true)
  val fakeAuthAction: FakeAuthAction                = new FakeAuthAction(Session("id", SessionData(1, Some(userData))))
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
        Person(fakePersonDetails(firstname = "Firstname4", id = 4, timestamp = timeStamp), Events(List.empty))
      val personE: Person =
        Person(fakePersonDetails(firstname = "Firstname5", id = 5, timestamp = timeStamp), Events(List.empty))

      val familyC: Family   = Family(1, None, Some(personE), timeStamp, None, "")
      val parentsC: Parents = Parents(familyC, "", "", None)
      val personC: Person =
        Person(
          fakePersonDetails(firstname = "Firstname3", id = 3, timestamp = timeStamp),
          Events(List.empty),
          parents = List(parentsC)
        )

      val familyB: Family   = Family(1, Some(personC), Some(personD), timeStamp, None, "")
      val parentsB: Parents = Parents(familyB, "", "", None)
      val personB: Person =
        Person(
          fakePersonDetails(firstname = "Firstname2", id = 2, timestamp = timeStamp),
          Events(List.empty),
          parents = List(parentsB)
        )

      val familyA: Family   = Family(1, Some(personB), None, timeStamp, None, "")
      val parentsA: Parents = Parents(familyA, "", "", None)
      val personA: Person =
        Person(
          fakePersonDetails(firstname = "Firstname1", id = 1, timestamp = timeStamp),
          Events(List.empty),
          parents = List(parentsA)
        )

      when(mockAscendanceService.getAscendant(any(), any())).thenReturn(
        Future.successful(Some(personA))
      )

      val expected =
        """
          |    <h3>Generation0</h3>
          |    <ul>
          |      <li><astyle="padding:0;margin:0"href="/individual/1">Firstname1Surname</a></li>
          |    </ul>
          |
          |  <h3>Generation1</h3>
          |  <ul>
          |    <li><astyle="padding:0;margin:0"href="/individual/2">Firstname2Surname</a></li>
          |  </ul>
          |
          |  <h3>Generation2</h3>
          |  <ul>
          |    <li><astyle="padding:0;margin:0"href="/individual/3">Firstname3Surname</a></li>
          |    <li><astyle="padding:0;margin:0"href="/individual/4">Firstname4Surname</a></li>
          |  </ul>
          |
          |  <h3>Generation3</h3>
          |  <ul>
          |    <li><astyle="padding:0;margin:0"href="/individual/5">Firstname5Surname</a></li>
          |  </ul>
          |""".stripMargin.filterNot(_.isWhitespace)

      val result = sut.showAscendant(1).apply(FakeRequest())
      status(result) mustBe OK
      contentAsString(result).filterNot(_.isWhitespace) must include(expected)
    }

    "remove duplicates" in {
      val personD: Person = Person(fakePersonDetails(firstname = "Firstname4", id = 4), Events(List.empty))
      val personE: Person = Person(fakePersonDetails(firstname = "Firstname5", id = 5), Events(List.empty))

      val familyC: Family   = Family(1, None, Some(personE), Instant.now, None, "")
      val parentsC: Parents = Parents(familyC, "", "", None)
      val personC: Person =
        Person(fakePersonDetails(firstname = "Firstname3", id = 3), Events(List.empty), parents = List(parentsC))

      val familyB: Family   = Family(1, Some(personC), Some(personD), Instant.now, None, "")
      val parentsB: Parents = Parents(familyB, "", "", None)
      val personB: Person =
        Person(fakePersonDetails(firstname = "Firstname2", id = 2), Events(List.empty), parents = List(parentsB))

      val familyBB: Family   = Family(1, Some(personC), Some(personD), Instant.now, None, "")
      val parentsBB: Parents = Parents(familyB, "", "", None)
      val personBB: Person =
        Person(fakePersonDetails(firstname = "Firstname22", id = 2), Events(List.empty), parents = List(parentsB))

      val familyA: Family   = Family(1, Some(personB), Some(personBB), Instant.now, None, "")
      val parentsA: Parents = Parents(familyA, "", "", None)
      val personA: Person =
        Person(fakePersonDetails(firstname = "Firstname1", id = 1), Events(List.empty), parents = List(parentsA))

      when(mockAscendanceService.getAscendant(any(), any())).thenReturn(
        Future.successful(Some(personA))
      )

      val expected =
        """
          |    <h3>Generation0</h3>
          |    <ul>
          |      <li><astyle="padding:0;margin:0"href="/individual/1">Firstname1Surname</a></li>
          |    </ul>
          |
          |    <h3>Generation 1</h3>
          |    <ul>
          |      <li><a style="padding:0;margin:0" href="/individual/2">Firstname2 Surname</a> </li>
          |      <li><a style="padding:0;margin:0" href="/individual/2">Firstname22 Surname</a> </li>
          |    </ul>
          |
          |    <h3>Generation 2</h3>
          |    <ul>
          |      <li><a style="padding:0;margin:0" href="/individual/3">Firstname3 Surname</a> </li>
          |      <li><a style="padding:0;margin:0" href="/individual/4">Firstname4 Surname</a> </li>
          |    </ul>
          |
          |    <h3>Generation 3</h3>
          |    <ul>
          |      <li><a style="padding:0;margin:0" href="/individual/5">Firstname5 Surname</a> </li>
          |    </ul>
          |""".stripMargin.filterNot(_.isWhitespace)

      val result = sut.showAscendant(1).apply(FakeRequest())
      status(result) mustBe OK
      contentAsString(result).filterNot(_.isWhitespace) must include(expected)
    }
  }
}
