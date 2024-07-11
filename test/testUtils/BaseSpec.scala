package testUtils

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.play.PlaySpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Application

trait BaseSpec extends PlaySpec with GuiceOneAppPerSuite {
  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .build()
}
