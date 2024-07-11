package models

import java.time.Instant

import org.scalatest.matchers.should.Matchers.should
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.play.*
import play.api.i18n.Lang
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.i18n.MessagesImpl
import testUtils.BaseSpec

class EventDetailsSpec extends BaseSpec {

  def EventDetailSut(
      base: Int = 0,
      events_details_id: Int = 0,
      place: Option[Place] = None,
      addr_id: Option[Int] = None,
      events_details_descriptor: String = "",
      events_details_gedcom_date: String = "",
      events_details_age: String = "",
      events_details_cause: String = "",
      jd_count: Option[Int] = None,
      jd_precision: Option[Int] = None,
      jd_calendar: Option[String] = None,
      events_details_famc: Option[Int] = None,
      events_details_adop: Option[String] = None,
      events_details_timestamp: Instant = Instant.now,
      tag: String = ""
  ): EventDetail = {
    EventDetail(
      base,
      events_details_id,
      place,
      addr_id,
      events_details_descriptor,
      events_details_gedcom_date,
      events_details_age,
      events_details_cause,
      jd_count,
      jd_precision,
      jd_calendar,
      events_details_famc,
      events_details_adop,
      events_details_timestamp,
      tag
    )
  }

  implicit def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit lazy val messages: Messages = MessagesImpl(Lang("en"), messagesApi)

  "formatDate" must {
    "format 5 APR 1204" in {
      val input    = "5 APR 1204"
      val expected = "5 April 1204"
      val sut      = EventDetailSut(events_details_gedcom_date = input)

      sut.formatDate(messages) shouldBe expected
    }

    "format APR 1204" in {
      val input    = "APR 1204"
      val expected = "April 1204"
      val sut      = EventDetailSut(events_details_gedcom_date = input)

      sut.formatDate(messages) shouldBe expected
    }

    "format BET FEB 1309 AND 4 DEC 1934" in {
      val input    = "BET FEB 1309 AND 4 DEC 1934"
      val expected = "Between February 1309 and 4 December 1934"
      val sut      = EventDetailSut(events_details_gedcom_date = input)

      sut.formatDate(messages) shouldBe expected
    }

    "format @#DJULIAN@ BEF MAY 2001" in {
      val input    = "@#DJULIAN@ BEF MAY 2001"
      val expected = "Before May 2001"
      val sut      = EventDetailSut(events_details_gedcom_date = input)

      sut.formatDate(messages) shouldBe expected
    }

    "format @#DFRENCH R@ 23 FRIM 2001" in {
      val input    = "@#DFRENCH R@ 23 FRIM 2001"
      val expected = "23 Frimaire 2001"
      val sut      = EventDetailSut(events_details_gedcom_date = input)

      sut.formatDate(messages) shouldBe expected
    }

    "format ABT 1728" in {
      val input    = "ABT 1728"
      val expected = "About 1728"
      val sut      = EventDetailSut(events_details_gedcom_date = input)

      sut.formatDate(messages) shouldBe expected
    }
  }

}
