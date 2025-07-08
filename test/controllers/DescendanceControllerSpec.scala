package controllers

import java.time.Instant
import java.time.LocalDateTime

import scala.concurrent.Future

import actions.AuthAction
import models.*
import models.EventType.FamilyEvent
import models.EventType.IndividualEvent
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
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
  val fakeAuthAction: FakeAuthAction             = new FakeAuthAction(Session("id", SessionData(Some(userData)), LocalDateTime.now))
  val mockDescendanceService: DescendanceService = mock[DescendanceService]

  val personD: Person =
    Person(
      fakePersonDetails(firstname = "Firstname4", id = 4),
      Events(List.empty, Some(4), IndividualEvent),
      Attributes(List.empty, Some(1), IndividualEvent)
    )
  val personE: Person =
    Person(
      fakePersonDetails(firstname = "Firstname5", id = 5),
      Events(List.empty, Some(5), IndividualEvent),
      Attributes(List.empty, Some(1), IndividualEvent)
    )

  val childD: Child = Child(personD, "relaType", None)
  val childE: Child = Child(personE, "relaType", None)

  val familyC: Family =
    Family(3, None, None, Instant.now, None, "", List(childE), Events(List.empty, Some(3), FamilyEvent))
  val personC: Person =
    Person(
      fakePersonDetails(firstname = "Firstname3", id = 3),
      Events(List.empty, Some(3), IndividualEvent),
      Attributes(List.empty, Some(1), IndividualEvent),
      families = List(familyC)
    )

  val childC: Child   = Child(personC, "relaType", None)
  val familyB: Family =
    Family(2, None, None, Instant.now, None, "", List(childC, childD), Events(List.empty, Some(2), FamilyEvent))
  val personB: Person =
    Person(
      fakePersonDetails(firstname = "Firstname2", id = 2),
      Events(List.empty, Some(2), IndividualEvent),
      Attributes(List.empty, Some(1), IndividualEvent),
      families = List(familyB)
    )

  val childB: Child   = Child(personB, "relaType", None)
  val familyA: Family =
    Family(1, None, None, Instant.now, None, "", List(childB), Events(List.empty, Some(1), FamilyEvent))
  val personA: Person =
    Person(
      fakePersonDetails(firstname = "Firstname1", id = 1),
      Events(List.empty, Some(1), IndividualEvent),
      Attributes(List.empty, Some(1), IndividualEvent),
      families = List(familyA)
    )

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
          |<divclass="govuk-!-padding-4box">
          |<ulclass="govuk-list">
          |  <listyle="margin:0;padding:0"><spanclass="govuk-!-font-size-16">[1]</span><aclass="govuk-link"style="padding:0;margin:0"href="/base/1/individual/1">Firstname1Surname</a>
          |    <ulclass="govuk-listgovuk-!-margin-top-0govuk-!-margin-bottom-0"style="padding-left:40px">
          |      <listyle="margin:0;padding:0">└<spanclass="govuk-!-font-size-16">[2]</span><aclass="govuk-link"style="padding:0;margin:0"href="/base/1/individual/2">Firstname2Surname</a>
          |        <ulclass="govuk-listgovuk-!-margin-top-0govuk-!-margin-bottom-0"style="padding-left:40px">
          |          <listyle="margin:0;padding:0">├<spanclass="govuk-!-font-size-16">[3]</span><aclass="govuk-link"style="padding:0;margin:0"href="/base/1/individual/3">Firstname3Surname</a>
          |            <ulclass="govuk-listgovuk-!-margin-top-0govuk-!-margin-bottom-0"style="padding-left:40px">
          |              <listyle="margin:0;padding:0">└<spanclass="govuk-!-font-size-16">[4]</span><aclass="govuk-link"style="padding:0;margin:0"href="/base/1/individual/5">Firstname5Surname</a></li>
          |            </ul>
          |          </li>
          |          <listyle="margin:0;padding:0">└<spanclass="govuk-!-font-size-16">[3]</span><aclass="govuk-link"style="padding:0;margin:0"href="/base/1/individual/4">Firstname4Surname</a></li>
          |        </ul>
          |      </li>
          |    </ul>
          |  </li>
          |</ul>
          |</div>
          |""".stripMargin.filterNot(_.isWhitespace)

      val result = sut.showDescendant(1, 1).apply(FakeRequest())
      status(result) mustBe OK
      contentAsString(result).filterNot(_.isWhitespace) must include(expected)
    }
  }
}
