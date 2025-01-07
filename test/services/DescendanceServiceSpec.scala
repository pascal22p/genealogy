package services

import java.time.Instant

import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

import models.*
import models.EventType.FamilyEvent
import models.EventType.IndividualEvent
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
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
import queries.GetSqlQueries
import testUtils.BaseSpec

class DescendanceServiceSpec extends BaseSpec {
  val sut: DescendanceService = app.injector.instanceOf[DescendanceService]

  lazy val mockPersonService: PersonService = mock[PersonService]

  protected override def localGuiceApplicationBuilder(): GuiceApplicationBuilder =
    GuiceApplicationBuilder()
      .overrides(
        bind[PersonService].toInstance(mockPersonService)
      )

  "getDescendant" must {
    "Return person tree" in {
      val timeStamp = Instant.now

      val personD: Person =
        Person(
          fakePersonDetails(firstname = "Firstname4", id = 4, timestamp = timeStamp),
          Events(List.empty, Some(4), IndividualEvent)
        )
      val personE: Person =
        Person(
          fakePersonDetails(firstname = "Firstname5", id = 5, timestamp = timeStamp),
          Events(List.empty, Some(5), IndividualEvent)
        )

      val childD: Child = Child(personD, "relaType", None)
      val childE: Child = Child(personE, "relaType", None)

      val familyC: Family =
        Family(1, None, None, timeStamp, None, "", List(childE), Events(List.empty, Some(1), FamilyEvent))
      val personC: Person =
        Person(
          fakePersonDetails(firstname = "Firstname3", id = 3, timestamp = timeStamp),
          Events(List.empty, Some(3), IndividualEvent),
          families = List(familyC)
        )

      val childC: Child = Child(personC.copy(families = List.empty), "relaType", None)
      val familyB: Family = Family(
        1,
        None,
        None,
        timeStamp,
        None,
        "",
        List(childC, childD),
        Events(List.empty, Some(1), FamilyEvent)
      )
      val personB: Person =
        Person(
          fakePersonDetails(firstname = "Firstname2", id = 2, timestamp = timeStamp),
          Events(List.empty, Some(2), IndividualEvent),
          families = List(familyB)
        )

      val childB: Child = Child(personB.copy(families = List.empty), "relaType", None)
      val familyA: Family =
        Family(1, None, None, timeStamp, None, "", List(childB), Events(List.empty, Some(1), FamilyEvent))
      val personA: Person =
        Person(
          fakePersonDetails(firstname = "Firstname1", id = 1, timestamp = timeStamp),
          Events(List.empty, Some(1), IndividualEvent),
          families = List(familyA)
        )

      val expected = Person(
        PersonDetails(
          1,
          1,
          "Firstname1",
          "Surname",
          MaleSex,
          timeStamp,
          "firstnamePrefix",
          "surnamePrefix",
          "nameSuffix",
          "nameGiven",
          "surnamePrefix",
          None
        ),
        Events(List.empty, Some(1), IndividualEvent),
        List(),
        List(
          Family(
            1,
            None,
            None,
            timeStamp,
            None,
            "",
            List(
              Child(
                Person(
                  PersonDetails(
                    1,
                    2,
                    "Firstname2",
                    "Surname",
                    MaleSex,
                    timeStamp,
                    "firstnamePrefix",
                    "surnamePrefix",
                    "nameSuffix",
                    "nameGiven",
                    "surnamePrefix",
                    None
                  ),
                  Events(List.empty, Some(2), IndividualEvent),
                  List(),
                  List(
                    Family(
                      1,
                      None,
                      None,
                      timeStamp,
                      None,
                      "",
                      List(
                        Child(
                          Person(
                            PersonDetails(
                              1,
                              3,
                              "Firstname3",
                              "Surname",
                              MaleSex,
                              timeStamp,
                              "firstnamePrefix",
                              "surnamePrefix",
                              "nameSuffix",
                              "nameGiven",
                              "surnamePrefix",
                              None
                            ),
                            Events(List.empty, Some(3), IndividualEvent),
                            List(),
                            List(
                              Family(
                                1,
                                None,
                                None,
                                timeStamp,
                                None,
                                "",
                                List(
                                  Child(
                                    Person(
                                      PersonDetails(
                                        1,
                                        5,
                                        "Firstname5",
                                        "Surname",
                                        MaleSex,
                                        timeStamp,
                                        "firstnamePrefix",
                                        "surnamePrefix",
                                        "nameSuffix",
                                        "nameGiven",
                                        "surnamePrefix",
                                        None
                                      ),
                                      Events(List.empty, Some(5), IndividualEvent),
                                      List(),
                                      List()
                                    ),
                                    "relaType",
                                    None
                                  )
                                ),
                                Events(List.empty, Some(1), FamilyEvent)
                              )
                            )
                          ),
                          "relaType",
                          None
                        ),
                        Child(
                          Person(
                            PersonDetails(
                              1,
                              4,
                              "Firstname4",
                              "Surname",
                              MaleSex,
                              timeStamp,
                              "firstnamePrefix",
                              "surnamePrefix",
                              "nameSuffix",
                              "nameGiven",
                              "surnamePrefix",
                              None
                            ),
                            Events(List.empty, Some(4), IndividualEvent),
                            List(),
                            List()
                          ),
                          "relaType",
                          None
                        )
                      ),
                      Events(List.empty, Some(1), FamilyEvent)
                    )
                  )
                ),
                "relaType",
                None
              )
            ),
            Events(List.empty, Some(1), FamilyEvent)
          )
        )
      )

      when(mockPersonService.getPerson(ArgumentMatchers.eq(1), any[Boolean], any[Boolean], any[Boolean]))
        .thenReturn(Future.successful(Some(personA)))
      when(mockPersonService.getPerson(ArgumentMatchers.eq(2), any[Boolean], any[Boolean], any[Boolean]))
        .thenReturn(Future.successful(Some(personB)))
      when(mockPersonService.getPerson(ArgumentMatchers.eq(3), any[Boolean], any[Boolean], any[Boolean]))
        .thenReturn(Future.successful(Some(personC)))
      when(mockPersonService.getPerson(ArgumentMatchers.eq(4), any[Boolean], any[Boolean], any[Boolean]))
        .thenReturn(Future.successful(Some(personD)))
      when(mockPersonService.getPerson(ArgumentMatchers.eq(5), any[Boolean], any[Boolean], any[Boolean]))
        .thenReturn(Future.successful(Some(personE)))

      val result: Option[Person] = sut.getDescendant(1, 0).futureValue
      result mustBe Some(expected)
    }
  }

}
