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
import services.DescendanceService
import testUtils.BaseSpec
import testUtils.FakeAuthAction

class DescendanceControllerSpec extends BaseSpec {
  val userData: UserData                         = UserData(1, "username", "hashedPassword", true, true)
  val fakeAuthAction: FakeAuthAction             = new FakeAuthAction(Session("id", SessionData(1, Some(userData))))
  val mockDescendanceService: DescendanceService = mock[DescendanceService]

  val personD: Person = Person(fakePersonDetails(firstname = "Firstname4", id = 4), Events(List.empty))
  val personE: Person = Person(fakePersonDetails(firstname = "Firstname5", id = 5), Events(List.empty))

  val childD: Child = Child(personD, "relaType", None)
  val childE: Child = Child(personE, "relaType", None)

  val familyC: Family = Family(1, None, None, Instant.now, None, "", List(childE))
  val personC: Person =
    Person(fakePersonDetails(firstname = "Firstname3", id = 3), Events(List.empty), families = List(familyC))

  val childC: Child   = Child(personC, "relaType", None)
  val familyB: Family = Family(1, None, None, Instant.now, None, "", List(childC, childD))
  val personB: Person =
    Person(fakePersonDetails(firstname = "Firstname2", id = 2), Events(List.empty), families = List(familyB))

  val childB: Child   = Child(personB, "relaType", None)
  val familyA: Family = Family(1, None, None, Instant.now, None, "", List(childB))
  val personA: Person =
    Person(fakePersonDetails(firstname = "Firstname1", id = 1), Events(List.empty), families = List(familyA))

  protected override def localGuiceApplicationBuilder(): GuiceApplicationBuilder =
    GuiceApplicationBuilder()
      .overrides(
        bind[AuthAction].toInstance(fakeAuthAction),
        bind[DescendanceService].toInstance(mockDescendanceService)
      )

  val sut: DescendanceController = app.injector.instanceOf[DescendanceController]

  "showDescendant" must {
    "display tree of children" in {
      when(mockDescendanceService.getDescendant(any(), any())).thenReturn(
        Future.successful(Some(personA))
      )

      val expected =
        """
          |       <ul><li style="margin:0;padding:0">
          |          <span style="font-size:x-small">[1]</span>
          |          <a style="padding:0;margin:0" href="/individual/1">Firstname1 Surname</a> <ul><li style="margin:0;padding:0">└<span style="font-size:x-small">[2]</span>
          |          <a style="padding:0;margin:0" href="/individual/2">Firstname2 Surname</a> <ul><li style="margin:0;padding:0">├<span style="font-size:x-small">[3]</span>
          |          <a style="padding:0;margin:0" href="/individual/3">Firstname3 Surname</a> <ul><li style="margin:0;padding:0">└<span style="font-size:x-small">[4]</span>
          |          <a style="padding:0;margin:0" href="/individual/5">Firstname5 Surname</a> </li></ul></li><li style="margin:0;padding:0">└<span style="font-size:x-small">[3]</span>
          |          <a style="padding:0;margin:0" href="/individual/4">Firstname4 Surname</a> </li></ul></li></ul>
          |       </li></ul>
          |""".stripMargin.filterNot(_.isWhitespace)

      val result = sut.showDescendant(1).apply(FakeRequest())
      status(result) mustBe OK
      contentAsString(result).filterNot(_.isWhitespace) must include(expected)
    }
  }
}
