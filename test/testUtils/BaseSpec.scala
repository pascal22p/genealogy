package testUtils

import java.time.Instant

import models.EventDetail
import models.MaleSex
import models.PersonDetails
import models.Place
import models.Sex
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.play.PlaySpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting
import play.api.Application

trait BaseSpec extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures with Injecting with IntegrationPatience {
  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .build()

  def fakePersonDetails(
      base: Int = 1,
      id: Int = 1,
      firstname: String = "Firstname",
      surname: String = "Surname", // SURN
      sex: Sex = MaleSex,          // SEX
      timestamp: Instant = Instant.now(),
      firstnamePrefix: String = "firstnamePrefix", // NPFX
      surnamePrefix: String = "surnamePrefix",     // SPFX
      nameSuffix: String = "nameSuffix",           // NSFX
      nameGiven: String = "nameGiven",             // GIVN
      nameNickname: String = "nameNickname",       // NICK
      privacyRestriction: Option[String] = None    // RESN

  ): PersonDetails = PersonDetails(
    base,
    id,
    firstname,
    surname,
    sex,
    timestamp,
    firstnamePrefix,
    surnamePrefix,
    nameSuffix,
    nameGiven,
    surnamePrefix,
    privacyRestriction
  )

  def fakeEventDetail(
      base: Int = 1,
      events_details_id: Int = 1,
      place: Option[Place] = None,
      addr_id: Option[Int] = None,
      events_details_descriptor: String = "BIRT",
      events_details_gedcom_date: String = "1 JAN 1901",
      events_details_age: String = "",
      events_details_cause: String = "",
      jd_count: Option[Int] = None,
      jd_precision: Option[Int] = None,
      jd_calendar: Option[String] = None,
      events_details_famc: Option[Int] = None,
      events_details_adop: Option[String] = None,
      events_details_timestamp: Instant = Instant.now,
      tag: String = ""
  ): EventDetail = EventDetail(
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
