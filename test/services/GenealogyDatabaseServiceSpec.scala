package services

import java.time.Instant

import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

import models.AuthenticatedRequest
import models.EventDetail
import models.Events
import models.FemaleSex
import models.Person
import models.PersonDetails
import models.Session
import models.SessionData
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.Application
import queries.MariadbQueries
import testUtils.BaseSpec

class GenealogyDatabaseServiceSpec extends BaseSpec {
  val sut: GenealogyDatabaseService = app.injector.instanceOf[GenealogyDatabaseService]
  val fakeAuthenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
    AuthenticatedRequest(FakeRequest(), Session("id", SessionData(1, None)))

  lazy val mockMariadbQueries: MariadbQueries             = mock[MariadbQueries]
  lazy val mockPersonDetailsService: PersonDetailsService = mock[PersonDetailsService]

  protected override def localGuiceApplicationBuilder(): GuiceApplicationBuilder =
    GuiceApplicationBuilder()
      .overrides(
        bind[MariadbQueries].toInstance(mockMariadbQueries),
        bind[PersonDetailsService].toInstance(mockPersonDetailsService)
      )

  "getFirstnamesList" must {
    "Return a list of names with birth/death" in {
      val argumentCaptor: ArgumentCaptor[Int] = ArgumentCaptor.forClass(classOf[Int])
      val fakePersonDetail1                   = fakePersonDetails(id = 1)
      val fakePersonDetail2                   = fakePersonDetails(id = 2)
      val fakeEventDetails1                   = List(fakeEventDetail(events_details_id = 1))
      val fakeEventDetails2                   = List(fakeEventDetail(events_details_id = 2))

      val person1 = Person(fakePersonDetail1, Events(fakeEventDetails1))
      val person2 = Person(fakePersonDetail2, Events(fakeEventDetails2))
      when(mockMariadbQueries.getFirstnamesList(any(), any())(any())).thenReturn(
        Future.successful(List(fakePersonDetail1, fakePersonDetail2))
      )
      when(mockPersonDetailsService.getIndividualEvents(any()))
        .thenReturn(Future.successful(fakeEventDetails1))
        .thenReturn(Future.successful(fakeEventDetails2))

      val result: List[Person] = sut.getFirstnamesList(1, "Test")(fakeAuthenticatedRequest).futureValue
      result mustBe List(person1, person2)
      verify(mockPersonDetailsService, times(2)).getIndividualEvents(argumentCaptor.capture())
      val capturedPersonIds = argumentCaptor.getAllValues.asScala.toList
      capturedPersonIds mustBe List(1, 2)
    }
  }

}
