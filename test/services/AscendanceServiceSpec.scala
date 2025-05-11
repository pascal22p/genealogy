package services

import java.time.Instant

import scala.concurrent.Future

import models.Attributes
import models.EventType.FamilyEvent
import models.EventType.IndividualEvent
import models.Events
import models.Family
import models.MaleSex
import models.Parents
import models.Person
import models.PersonDetails
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import testUtils.BaseSpec

class AscendanceServiceSpec extends BaseSpec {
  val sut: AscendanceService = app.injector.instanceOf[AscendanceService]

  lazy val mockPersonService: PersonService = mock[PersonService]

  protected override def localGuiceApplicationBuilder(): GuiceApplicationBuilder =
    GuiceApplicationBuilder()
      .overrides(
        bind[PersonService].toInstance(mockPersonService)
      )

  "getAscendant" must {
    "return list of ascendants" in {
      val timeStamp = Instant.now

      val personD: Person =
        Person(
          fakePersonDetails(firstname = "Firstname4", id = 4, timestamp = timeStamp),
          Events(List.empty, Some(4), IndividualEvent),
          Attributes(List.empty, Some(1), IndividualEvent)
        )
      val personE: Person =
        Person(
          fakePersonDetails(firstname = "Firstname5", id = 5, timestamp = timeStamp),
          Events(List.empty, Some(5), IndividualEvent),
          Attributes(List.empty, Some(1), IndividualEvent)
        )

      val familyC: Family = Family(
        1,
        None,
        Some(personE.copy(parents = List.empty)),
        timeStamp,
        None,
        "",
        List.empty,
        Events(List.empty, Some(1), FamilyEvent)
      )
      val parentsC: Parents = Parents(familyC, "", "", None)
      val personC: Person =
        Person(
          fakePersonDetails(firstname = "Firstname3", id = 3, timestamp = timeStamp),
          Events(List.empty, Some(3), IndividualEvent),
          Attributes(List.empty, Some(1), IndividualEvent),
          parents = List(parentsC)
        )

      val familyB: Family = Family(
        1,
        Some(personC.copy(parents = List.empty)),
        Some(personD.copy(parents = List.empty)),
        timeStamp,
        None,
        "",
        List.empty,
        Events(List.empty, Some(1), FamilyEvent)
      )
      val parentsB: Parents = Parents(familyB, "", "", None)
      val personB: Person =
        Person(
          fakePersonDetails(firstname = "Firstname2", id = 2, timestamp = timeStamp),
          Events(List.empty, Some(2), IndividualEvent),
          Attributes(List.empty, Some(1), IndividualEvent),
          parents = List(parentsB)
        )

      val familyA: Family =
        Family(
          1,
          Some(personB.copy(parents = List.empty)),
          None,
          timeStamp,
          None,
          "",
          List.empty,
          Events(List.empty, Some(1), FamilyEvent)
        )
      val parentsA: Parents = Parents(familyA, "", "", None)
      val personA: Person =
        Person(
          fakePersonDetails(firstname = "Firstname1", id = 1, timestamp = timeStamp),
          Events(List.empty, Some(1), IndividualEvent),
          Attributes(List.empty, Some(1), IndividualEvent),
          parents = List(parentsA)
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
          "nameNickname",
          None
        ),
        Events(List.empty, Some(1), IndividualEvent),
        Attributes(List.empty, Some(1), IndividualEvent),
        List(
          Parents(
            Family(
              1,
              Some(
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
                    "nameNickname",
                    None
                  ),
                  Events(List.empty, Some(2), IndividualEvent),
                  Attributes(List.empty, Some(1), IndividualEvent),
                  List(
                    Parents(
                      Family(
                        1,
                        Some(
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
                              "nameNickname",
                              None
                            ),
                            Events(List.empty, Some(3), IndividualEvent),
                            Attributes(List.empty, Some(1), IndividualEvent),
                            List(
                              Parents(
                                Family(
                                  1,
                                  None,
                                  Some(
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
                                        "nameNickname",
                                        None
                                      ),
                                      Events(List.empty, Some(5), IndividualEvent),
                                      Attributes(List.empty, Some(1), IndividualEvent),
                                      List(),
                                      List()
                                    )
                                  ),
                                  timeStamp,
                                  None,
                                  "",
                                  List(),
                                  Events(List.empty, Some(1), FamilyEvent)
                                ),
                                "",
                                "",
                                None
                              )
                            ),
                            List()
                          )
                        ),
                        Some(
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
                              "nameNickname",
                              None
                            ),
                            Events(List.empty, Some(4), IndividualEvent),
                            Attributes(List.empty, Some(1), IndividualEvent),
                            List(),
                            List()
                          )
                        ),
                        timeStamp,
                        None,
                        "",
                        List(),
                        Events(List.empty, Some(1), FamilyEvent)
                      ),
                      "",
                      "",
                      None
                    )
                  ),
                  List()
                )
              ),
              None,
              timeStamp,
              None,
              "",
              List(),
              Events(List.empty, Some(1), FamilyEvent)
            ),
            "",
            "",
            None
          )
        ),
        List()
      )

      val result: Option[Person] = sut.getAscendant(1, 0).futureValue
      result mustBe Some(expected)
    }
  }

}
