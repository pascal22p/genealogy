package services

import java.time.LocalDateTime

import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

import models.Attributes
import models.AuthenticatedRequest
import models.EventType.IndividualEvent
import models.Events
import models.Person
import models.Session
import models.SessionData
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import queries.GetSqlQueries
import testUtils.BaseSpec

class GenealogyDatabaseServiceSpec extends BaseSpec {
  val sut: GenealogyDatabaseService = app.injector.instanceOf[GenealogyDatabaseService]
  val fakeAuthenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
    AuthenticatedRequest(FakeRequest(), Session("id", SessionData(None), LocalDateTime.now))

  lazy val mockMariadbQueries: GetSqlQueries              = mock[GetSqlQueries]
  lazy val mockPersonDetailsService: PersonDetailsService = mock[PersonDetailsService]
  lazy val mockEventService: EventService                 = mock[EventService]

  protected override def localGuiceApplicationBuilder(): GuiceApplicationBuilder =
    GuiceApplicationBuilder()
      .overrides(
        bind[GetSqlQueries].toInstance(mockMariadbQueries),
        bind[PersonDetailsService].toInstance(mockPersonDetailsService),
        bind[EventService].toInstance(mockEventService)
      )

  "getFirstnamesList" must {
    "Return a list of names with birth/death" in {
      val argumentCaptor: ArgumentCaptor[Int] = ArgumentCaptor.forClass(classOf[Int])
      val fakePersonDetail1                   = fakePersonDetails(id = 1)
      val fakePersonDetail2                   = fakePersonDetails(id = 2)
      val fakeEventDetails1                   = List(fakeEventDetail(events_details_id = 1))
      val fakeEventDetails2                   = List(fakeEventDetail(events_details_id = 2))

      val person1 = Person(
        fakePersonDetail1,
        Events(fakeEventDetails1, Some(1), IndividualEvent),
        Attributes(List.empty, Some(1), IndividualEvent)
      )
      val person2 = Person(
        fakePersonDetail2,
        Events(fakeEventDetails2, Some(2), IndividualEvent),
        Attributes(List.empty, Some(2), IndividualEvent)
      )
      when(mockMariadbQueries.getFirstnamesList(any(), any())(any())).thenReturn(
        Future.successful(List(fakePersonDetail1, fakePersonDetail2))
      )
      when(mockEventService.getIndividualEvents(any(), any[Boolean]))
        .thenReturn(Future.successful(fakeEventDetails1))
        .thenReturn(Future.successful(fakeEventDetails2))

      val result: List[Person] = sut.getFirstnamesList(1, "Test")(fakeAuthenticatedRequest).futureValue
      result mustBe List(person1, person2)
      verify(mockEventService, times(2)).getIndividualEvents(argumentCaptor.capture(), any[Boolean])
      val capturedPersonIds = argumentCaptor.getAllValues.asScala.toList
      capturedPersonIds mustBe List(1, 2)
    }
  }

}
