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
          |<divclass="govuk-!-padding-4box">
          |  <divclass="govuk-summary-cardgovuk-!-margin-4">
          |    <divclass="govuk-summary-card__title-wrapper">
          |      <h2class="govuk-summary-card__title">Generation0</h2>
          |    </div>
          |    <divclass="govuk-summary-card__content">
          |      <dlclass="govuk-summary-list">
          |        <divclass="govuk-summary-list__rowgovuk-summary-list__row--no-border">
          |          <dtclass="govuk-summary-list__key">Firstname1Surname</dt>
          |          <ddclass="govuk-summary-list__value"></dd>
          |          <ddclass="govuk-summary-list__actions"><aclass="govuk-link"href="/individual/1">Details<spanclass="govuk-visually-hidden">(Generation0)</span></a></dd>
          |        </div>
          |      </dl>
          |    </div>
          |  </div>
          |  <divclass="govuk-summary-cardgovuk-!-margin-4">
          |    <divclass="govuk-summary-card__title-wrapper">
          |      <h2class="govuk-summary-card__title">Generation1</h2>
          |    </div>
          |    <divclass="govuk-summary-card__content">
          |      <dlclass="govuk-summary-list">
          |        <divclass="govuk-summary-list__rowgovuk-summary-list__row--no-border">
          |          <dtclass="govuk-summary-list__key">Firstname2Surname</dt>
          |          <ddclass="govuk-summary-list__value"></dd>
          |          <ddclass="govuk-summary-list__actions"><aclass="govuk-link"href="/individual/2">Details<spanclass="govuk-visually-hidden">(Generation1)</span></a></dd>
          |        </div>
          |      </dl>
          |    </div>
          |  </div>
          |  <divclass="govuk-summary-cardgovuk-!-margin-4">
          |    <divclass="govuk-summary-card__title-wrapper">
          |      <h2class="govuk-summary-card__title">Generation2</h2>
          |    </div>
          |    <divclass="govuk-summary-card__content">
          |      <dlclass="govuk-summary-list">
          |        <divclass="govuk-summary-list__rowgovuk-summary-list__row--no-border">
          |          <dtclass="govuk-summary-list__key">Firstname3Surname</dt>
          |          <ddclass="govuk-summary-list__value"></dd><ddclass="govuk-summary-list__actions"><aclass="govuk-link"href="/individual/3">Details<spanclass="govuk-visually-hidden">(Generation2)</span></a></dd>
          |        </div>
          |        <divclass="govuk-summary-list__rowgovuk-summary-list__row--no-border">
          |          <dtclass="govuk-summary-list__key">Firstname4Surname</dt>
          |          <ddclass="govuk-summary-list__value"></dd>
          |          <ddclass="govuk-summary-list__actions"><aclass="govuk-link"href="/individual/4">Details<spanclass="govuk-visually-hidden">(Generation2)</span></a></dd>
          |        </div>
          |      </dl>
          |    </div>
          |  </div>
          |  <divclass="govuk-summary-cardgovuk-!-margin-4">
          |    <divclass="govuk-summary-card__title-wrapper">
          |      <h2class="govuk-summary-card__title">Generation3</h2>
          |    </div>
          |    <divclass="govuk-summary-card__content">
          |      <dlclass="govuk-summary-list">
          |        <divclass="govuk-summary-list__rowgovuk-summary-list__row--no-border">
          |          <dtclass="govuk-summary-list__key">Firstname5Surname</dt>
          |          <ddclass="govuk-summary-list__value"></dd>
          |          <ddclass="govuk-summary-list__actions"><aclass="govuk-link"href="/individual/5">Details<spanclass="govuk-visually-hidden">(Generation3)</span></a></dd>
          |        </div>
          |      </dl>
          |    </div>
          |  </div>
          |</div>
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
          | <divclass="govuk-!-padding-4box">
          |  <divclass="govuk-summary-cardgovuk-!-margin-4">
          |    <divclass="govuk-summary-card__title-wrapper">
          |      <h2class="govuk-summary-card__title">Generation0</h2>
          |    </div>
          |    <divclass="govuk-summary-card__content">
          |      <dlclass="govuk-summary-list">
          |        <divclass="govuk-summary-list__rowgovuk-summary-list__row--no-border">
          |          <dtclass="govuk-summary-list__key">Firstname1Surname</dt>
          |          <ddclass="govuk-summary-list__value"></dd>
          |          <ddclass="govuk-summary-list__actions"><aclass="govuk-link"href="/individual/1">Details<spanclass="govuk-visually-hidden">(Generation0)</span></a></dd>
          |        </div>
          |      </dl>
          |    </div>
          |  </div>
          |  <divclass="govuk-summary-cardgovuk-!-margin-4">
          |    <divclass="govuk-summary-card__title-wrapper">
          |      <h2class="govuk-summary-card__title">Generation1</h2>
          |    </div>
          |    <divclass="govuk-summary-card__content">
          |      <dlclass="govuk-summary-list">
          |        <divclass="govuk-summary-list__rowgovuk-summary-list__row--no-border">
          |          <dtclass="govuk-summary-list__key">Firstname2Surname</dt>
          |          <ddclass="govuk-summary-list__value"></dd>
          |          <ddclass="govuk-summary-list__actions"><aclass="govuk-link"href="/individual/2">Details<spanclass="govuk-visually-hidden">(Generation1)</span></a></dd>
          |        </div>
          |        <divclass="govuk-summary-list__rowgovuk-summary-list__row--no-border">
          |          <dtclass="govuk-summary-list__key">Firstname22Surname</dt>
          |            <ddclass="govuk-summary-list__value"></dd><ddclass="govuk-summary-list__actions"><aclass="govuk-link"href="/individual/2">Details<spanclass="govuk-visually-hidden">(Generation1)</span></a></dd>
          |        </div>
          |      </dl>
          |    </div>
          |  </div>
          |  <divclass="govuk-summary-cardgovuk-!-margin-4">
          |    <divclass="govuk-summary-card__title-wrapper">
          |      <h2class="govuk-summary-card__title">Generation2</h2>
          |    </div>
          |    <divclass="govuk-summary-card__content">
          |      <dlclass="govuk-summary-list">
          |        <divclass="govuk-summary-list__rowgovuk-summary-list__row--no-border">
          |          <dtclass="govuk-summary-list__key">Firstname3Surname</dt>
          |          <ddclass="govuk-summary-list__value"></dd>
          |          <ddclass="govuk-summary-list__actions"><aclass="govuk-link"href="/individual/3">Details<spanclass="govuk-visually-hidden">(Generation2)</span></a></dd>
          |        </div>
          |        <divclass="govuk-summary-list__rowgovuk-summary-list__row--no-border">
          |          <dtclass="govuk-summary-list__key">Firstname4Surname</dt>
          |          <ddclass="govuk-summary-list__value"></dd>
          |          <ddclass="govuk-summary-list__actions"><aclass="govuk-link"href="/individual/4">Details<spanclass="govuk-visually-hidden">(Generation2)</span></a></dd>
          |        </div>
          |      </dl>
          |    </div>
          |  </div>
          |  <divclass="govuk-summary-cardgovuk-!-margin-4">
          |    <divclass="govuk-summary-card__title-wrapper">
          |      <h2class="govuk-summary-card__title">Generation3</h2>
          |    </div>
          |    <divclass="govuk-summary-card__content">
          |      <dlclass="govuk-summary-list">
          |        <divclass="govuk-summary-list__rowgovuk-summary-list__row--no-border">
          |          <dtclass="govuk-summary-list__key">Firstname5Surname</dt>
          |          <ddclass="govuk-summary-list__value"></dd>
          |          <ddclass="govuk-summary-list__actions"><aclass="govuk-link"href="/individual/5">Details<spanclass="govuk-visually-hidden">(Generation3)</span></a></dd>
          |        </div>
          |      </dl>
          |    </div>
          |  </div>
          |</div>
          |
          |""".stripMargin.filterNot(_.isWhitespace)

      val result = sut.showAscendant(1).apply(FakeRequest())
      status(result) mustBe OK
      contentAsString(result).filterNot(_.isWhitespace) must include(expected)
    }
  }
}
