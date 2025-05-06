package models

import java.time.LocalDateTime

import config.AppConfig
import org.scalatest.matchers.should.Matchers.should
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.play.*
import play.api.i18n.Lang
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.i18n.MessagesImpl
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import testUtils.BaseSpec

class EventDetailsSpec extends BaseSpec {

  implicit def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit lazy val messages: Messages = MessagesImpl(Lang("en"), messagesApi)
  implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] =
    AuthenticatedRequest(
      FakeRequest(),
      Session("", SessionData(Some(UserData(0, "", "", true, true))), LocalDateTime.now)
    )
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  "formatDate" must {
    "format 5 APR 1204" in {
      val input    = "5 APR 1204"
      val expected = "5 April 1204"
      val sut      = fakeEventDetail(events_details_gedcom_date = input)

      sut.formatDate(false)(messages) shouldBe expected
    }

    "format APR 1204" in {
      val input    = "APR 1204"
      val expected = "April 1204"
      val sut      = fakeEventDetail(events_details_gedcom_date = input)

      sut.formatDate(false)(messages) shouldBe expected
    }

    "format BET FEB 1309 AND 4 DEC 1934" in {
      val input    = "BET FEB 1309 AND 4 DEC 1934"
      val expected = "Between February 1309 and 4 December 1934"
      val sut      = fakeEventDetail(events_details_gedcom_date = input)

      sut.formatDate(false)(messages) shouldBe expected
    }

    "format @#DJULIAN@ BEF MAY 2001" in {
      val input    = "@#DJULIAN@ BEF MAY 2001"
      val expected = "Before May 2001"
      val sut      = fakeEventDetail(events_details_gedcom_date = input)

      sut.formatDate(false)(messages) shouldBe expected
    }

    "format @#DFRENCH R@ 23 FRIM 2001" in {
      val input    = "@#DFRENCH R@ 23 FRIM 2001"
      val expected = "23 Frimaire 2001"
      val sut      = fakeEventDetail(events_details_gedcom_date = input)

      sut.formatDate(false)(messages) shouldBe expected
    }

    "format ABT 1728" in {
      val input    = "ABT 1728"
      val expected = "About 1728"
      val sut      = fakeEventDetail(events_details_gedcom_date = input)

      sut.formatDate(false)(messages) shouldBe expected
    }

    "format @#DFRENCH R@ 23 FRIM 2001 using short month" in {
      val input    = "@#DFRENCH R@ 23 FRIM 2001"
      val expected = "23 Frim 2001"
      val sut      = fakeEventDetail(events_details_gedcom_date = input)

      sut.formatDate(true)(messages) shouldBe expected
    }

    "redact date" when {
      "a simple year is entered" in {
        implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] =
          AuthenticatedRequest(
            FakeRequest(),
            Session("", SessionData(Some(UserData(0, "", "", false, false))), LocalDateTime.now)
          )

        val input    = "1 JAN 1928"
        val expected = appConfig.redactedMask
        val sut      = fakeEventDetail(events_details_gedcom_date = input)

        sut.formatDate(false)(messages, request) shouldBe expected

      }
      "a date range is entered" in {
        implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] =
          AuthenticatedRequest(
            FakeRequest(),
            Session("", SessionData(Some(UserData(0, "", "", false, false))), LocalDateTime.now)
          )

        val input    = "BET 1 JAN 1728 AND 2 FEB 2005"
        val expected = appConfig.redactedMask
        val sut      = fakeEventDetail(events_details_gedcom_date = input)

        sut.formatDate(false)(messages, request) shouldBe expected
      }
    }
  }

}
