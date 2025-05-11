package queries

import java.time.LocalDateTime

import models.*
import models.queryData.EventDetailQueryData
import models.queryData.FamilyAsChildQueryData
import models.queryData.FamilyQueryData
import models.EventType.FamilyEvent
import models.EventType.IndividualEvent
import models.ResnType.PrivacyResn
import play.api.test.FakeRequest
import play.api.Logging
import testUtils.MariadbHelper

class GetSqlQueriesSpec extends MariadbHelper with Logging {
  lazy val sut: GetSqlQueries = app.injector.instanceOf[GetSqlQueries]

  def sqlPersonDetails(person: PersonDetails): String =
    s"""INSERT INTO `genea_individuals` (`indi_id`, `base`, `indi_nom`, `indi_prenom`, `indi_sexe`, `indi_npfx`, `indi_givn`, `indi_nick`, `indi_spfx`, `indi_nsfx`, `indi_resn`) VALUES
       |(${person.id},	${person.base},	'${person.surname}',	'${person.firstname}',	'${person.sex.gedcom}',	'${person.firstnamePrefix}',	'${person.nameGiven}',	'${person.nameNickname}',	'${person.surnamePrefix}',	'${person.nameSuffix}',	${person.privacyRestriction.fold("NULL")(r => s"'$r'")});
       |""".stripMargin

  def sqlEventDetails(event: EventDetail): String =
    s"""INSERT INTO `genea_events_details` (`events_details_id`, `place_id`, `addr_id`, `events_details_descriptor`, `events_details_gedcom_date`, `events_details_age`, `events_details_cause`, `jd_count`, `jd_precision`, `jd_calendar`, `events_details_famc`, `events_details_adop`, `base`) VALUES
       |(${event.events_details_id},	1,	NULL,	'',	'${event.events_details_gedcom_date}',	'',	'',	${event.jd_count.getOrElse("NULL")},	3,	'@#DGREGORIAN@',	NULL,	NULL,	1);
       |""".stripMargin

  def sqlFamilyDetails(id: Int, person1: PersonDetails, person2: PersonDetails): String =
    sqlPersonDetails(person1) +
      sqlPersonDetails(person2) +
      s"""INSERT INTO `genea_familles` (`familles_id`, `base`, `familles_husb`, `familles_wife`, `familles_resn`, `familles_refn`, `familles_refn_type`) VALUES
         |($id,	1,	${person1.id},	${person2.id},	NULL,	'',	'');""".stripMargin

  def sqlIndividualEvent(person: PersonDetails, event: EventDetail, eventTag: String): String =
    sqlPersonDetails(person) +
      sqlEventDetails(event) +
      s"""
         |INSERT INTO `rel_indi_events` (`events_details_id`, `indi_id`, `events_tag`, `events_attestation`) VALUES
         |(${event.events_details_id},	${person.id},	'$eventTag',	NULL);
         |""".stripMargin

  def sqlIndividualEvents(person: PersonDetails, events: List[EventDetail], eventTag: String): String = {
    sqlPersonDetails(person) +
      events
        .map { event =>
          sqlEventDetails(event) +
            s"""
               |INSERT INTO `rel_indi_events` (`events_details_id`, `indi_id`, `events_tag`, `events_attestation`) VALUES
               |(${event.events_details_id},	${person.id},	'$eventTag',	NULL);
               |""".stripMargin
        }
        .mkString("\n")
  }

  def sqlLinkFamilyEvent(idFamily: Int, event: EventDetail): String =
    s"""
       |INSERT INTO `rel_familles_events` (`events_details_id`, `familles_id`, `events_tag`, `events_attestation`) VALUES
       |(${event.events_details_id},	$idFamily,	${event.tag.fold("NULL")(t => s"'$t'")},	NULL);
       |""".stripMargin

  def sqlChild(child: Child, idFamily: Int): String =
    sqlPersonDetails(child.person.details) +
      s"""
         |INSERT INTO `rel_familles_indi` (`indi_id`, `familles_id`, `rela_type`) VALUES
         |(${child.person.details.id},	$idFamily, '${child.relaType}');
         |""".stripMargin

  def sqlGenealogyDatabases(id: Int): String =
    s"""
       |INSERT INTO `genea_infos` (`id`, `nom`, `descriptif`, `entetes`, `ged_corp`, `subm`) VALUES
       |($id,	'Name',	'Description',	'',	'',	NULL);
       |""".stripMargin

  def sqlFamily(
      idFamily: Int,
      person1: PersonDetails,
      person2: PersonDetails,
      children: List[Child],
      events: List[EventDetail]
  ): String = {
    sqlFamilyDetails(idFamily, person1, person2) +
      events.foldLeft("") { case (sql, event) => sql + sqlEventDetails(event) + sqlLinkFamilyEvent(idFamily, event) } +
      children.foldLeft("") { case (sql, child) => sql + sqlChild(child, idFamily) }
  }

  def sqlPlace(place: Place): String =
    s"""
       |INSERT INTO `genea_place` (`place_id`, `place_lieudit`, `place_ville`, `place_cp`, `place_insee`, `place_departement`, `place_region`, `place_pays`, `place_longitude`, `place_latitude`, `base`) VALUES
       |(${place.id},	'${place.lieuDit}',	'${place.city}',	'${place.postCode}',	${place.inseeNumber.getOrElse("NULL")},	'${place.county}',	'${place.region}',	'${place.country}',	${place.longitude.getOrElse("NULL")},	${place.latitude.getOrElse("NULL")},	${place.base});""".stripMargin

  "getPersonDetails" must {
    "returns person details" in {
      val person = fakePersonDetails(id = 1)
      val result = (for {
        _      <- executeSql(sqlPersonDetails(person))
        result <- sut.getPersonDetails(person.id)
      } yield result).futureValue

      result mustBe a[List[PersonDetails]]
      result.size mustBe 1
      result.head.id mustBe person.id
    }

    "returns nothing" in {
      val idPerson = 1
      val result   = sut.getPersonDetails(idPerson).futureValue

      result mustBe List.empty[PersonDetails]
    }
  }

  "getIndividualEvents" must {
    "returns individual events" in {
      val person   = fakePersonDetails(id = 1)
      val event    = fakeEventDetail(events_details_id = 2)
      val eventTag = "BIRT"
      val result = (for {
        _      <- executeSql(sqlIndividualEvent(person, event, eventTag))
        result <- sut.getEvents(person.id, IndividualEvent)
      } yield result).futureValue

      result mustBe a[List[EventDetailQueryData]]
      result.size mustBe 1
      result.head.events_details_id mustBe event.events_details_id
      result.head.tag mustBe Some(eventTag)
    }

    "returns nothing" in {
      val result = sut.getEvents(1, IndividualEvent).futureValue

      result mustBe List.empty[EventDetailQueryData]
    }
  }

  "getFamilyEvents" must {
    "returns family events" in {
      val person1  = fakePersonDetails(id = 1)
      val person2  = fakePersonDetails(id = 2)
      val idFamily = 3
      val event    = fakeEventDetail(events_details_id = 4, tag = Some("BIRT"))
      val result = (for {
        _      <- executeSql(sqlFamily(idFamily, person1, person2, List.empty, List(event)))
        result <- sut.getEvents(idFamily, FamilyEvent)
      } yield result).futureValue

      result mustBe a[List[EventDetailQueryData]]
      result.size mustBe 1
      result.head.events_details_id mustBe event.events_details_id
      result.head.tag mustBe event.tag
    }

    "returns nothing" in {
      val result = sut.getEvents(1, FamilyEvent).futureValue

      result mustBe List.empty[EventDetailQueryData]
    }
  }

  "getFamiliesFromIndividualId" must {
    "returns families" in {
      val person1  = fakePersonDetails(id = 1)
      val person2  = fakePersonDetails(id = 2)
      val idFamily = 3
      val child =
        Child(
          Person(
            fakePersonDetails(id = 5),
            Events(List.empty, Some(5), IndividualEvent),
            Attributes(List.empty, Some(1), IndividualEvent)
          ),
          "adopted",
          None
        )
      val result = (for {
        _      <- executeSql(sqlFamily(idFamily, person1, person2, List(child), List.empty))
        result <- sut.getFamiliesFromIndividualId(child.person.details.id)
      } yield result).futureValue

      result mustBe a[List[FamilyAsChildQueryData]]
      result.size mustBe 1
      result.head.family.parent1 mustBe Some(person1.id)
      result.head.family.parent2 mustBe Some(person2.id)
      result.head.relaType mustBe child.relaType
    }

    "returns nothing" in {
      val idPerson = 1
      val result   = sut.getFamiliesFromIndividualId(idPerson).futureValue

      result mustBe List.empty[FamilyAsChildQueryData]
    }
  }

  "getFamiliesAsPartner" must {
    "returns family when person is husb" in {
      val person1  = fakePersonDetails(id = 1)
      val person2  = fakePersonDetails(id = 2)
      val idFamily = 3
      val result = (for {
        _      <- executeSql(sqlFamily(idFamily, person1, person2, List.empty, List.empty))
        result <- sut.getFamilyIdsFromPartnerId(person1.id)
      } yield result).futureValue

      result mustBe List(3)
    }

    "returns family when person is wife" in {
      val person1  = fakePersonDetails(id = 1)
      val person2  = fakePersonDetails(id = 2)
      val idFamily = 3
      val result = (for {
        _      <- executeSql(sqlFamily(idFamily, person1, person2, List.empty, List.empty))
        result <- sut.getFamilyIdsFromPartnerId(person2.id)
      } yield result).futureValue

      result mustBe List(3)
    }

    "returns nothing" in {
      val idPerson = 1
      val result   = sut.getFamilyIdsFromPartnerId(idPerson).futureValue

      result mustBe List.empty
    }
  }

  "getFamilyDetails" must {
    "returns family" in {
      val person1  = fakePersonDetails(id = 1)
      val person2  = fakePersonDetails(id = 2)
      val idFamily = 3
      val result = (for {
        _      <- executeSql(sqlFamily(idFamily, person1, person2, List.empty, List.empty))
        result <- sut.getFamilyDetails(idFamily)
      } yield result).futureValue

      result mustBe a[Some[FamilyQueryData]]
      result.size mustBe 1
      result.head.parent1 mustBe Some(person1.id)
      result.head.parent2 mustBe Some(person2.id)
    }

    "returns nothing" in {
      val idPerson = 1
      val result   = sut.getFamilyDetails(idPerson).futureValue

      result mustBe None
    }
  }

  "getChildren" must {
    "returns list of children" in {
      val person1  = fakePersonDetails(id = 1)
      val person2  = fakePersonDetails(id = 2)
      val idFamily = 3
      val child =
        Child(
          Person(
            fakePersonDetails(id = 4),
            Events(List.empty, Some(4), IndividualEvent),
            Attributes(List.empty, Some(1), IndividualEvent)
          ),
          "adopted",
          None
        )
      val result = (for {
        _      <- executeSql(sqlFamily(idFamily, person1, person2, List(child), List.empty))
        result <- sut.getChildren(idFamily)
      } yield result).futureValue

      result mustBe a[List[Child]]
      result.size mustBe 1
      result.head.person.details.id mustBe child.person.details.id
      result.head.relaType mustBe "adopted"
    }

    "returns nothing" in {
      val idPerson = 1
      val result   = sut.getChildren(idPerson).futureValue

      result mustBe List.empty
    }
  }

  "getPlace" must {
    "returns a place" in {
      val place = fakePlace(id = 1)
      val result = (for {
        _      <- executeSql(sqlPlace(place))
        result <- sut.getPlace(place.id)
      } yield result).futureValue

      result mustBe Some(place)
    }

    "returns nothing" in {
      val idPerson = 1
      val result   = sut.getPlace(idPerson).futureValue

      result mustBe None
    }
  }

  "getGenealogyDatabases" must {
    "returns a list of databases" in {
      val result = (for {
        _      <- executeSql(sqlGenealogyDatabases(1))
        result <- sut.getGenealogyDatabases
      } yield result).futureValue

      result mustBe a[List[GenealogyDatabase]]
      result.size mustBe 1
    }

    "returns nothing" in {
      val result = sut.getGenealogyDatabases.futureValue

      result mustBe List.empty
    }
  }

  "getSurnamesList" must {
    "returns a list of names where restriction is None" in {
      val result = (for {
        _ <- executeSql(
          sqlPersonDetails(fakePersonDetails(id = 1, surname = "D")) +
            sqlPersonDetails(fakePersonDetails(id = 2, surname = "B")) +
            sqlPersonDetails(fakePersonDetails(id = 3, surname = "C")) +
            sqlPersonDetails(fakePersonDetails(id = 4, surname = "A", privacyRestriction = Some(PrivacyResn)))
        )
        result <- sut.getSurnamesList(1)(
          using AuthenticatedRequest(FakeRequest(), Session("sessionId", SessionData(None), LocalDateTime.now))
        )
      } yield result).futureValue

      result mustBe List(("B", 1, None, None), ("C", 1, None, None), ("D", 1, None, None))
    }

    "returns a list of names with start year and end year" in {
      val eventTag = "BIRT"
      val person1  = fakePersonDetails(id = 1, surname = "D")
      val event1   = fakeEventDetail(events_details_id = 11)
      val person2  = fakePersonDetails(id = 2, surname = "B")
      val event2   = fakeEventDetail(events_details_id = 22)
      val person3  = fakePersonDetails(id = 3, surname = "C")
      val event3   = fakeEventDetail(events_details_id = 33, jd_count = None)
      val person4  = fakePersonDetails(id = 4, surname = "A")
      val event4   = fakeEventDetail(events_details_id = 44, jd_count = Some(100))
      val event5   = fakeEventDetail(events_details_id = 55, jd_count = Some(1000))
      val event6   = fakeEventDetail(events_details_id = 66, jd_count = Some(10000))

      val result = (for {
        _ <- executeSql(sqlIndividualEvent(person1, event1, eventTag))
        _ <- executeSql(sqlIndividualEvent(person2, event2, eventTag))
        _ <- executeSql(sqlIndividualEvent(person3, event3, eventTag))
        _ <- executeSql(sqlIndividualEvents(person4, List(event4, event5, event6), eventTag))
        result <- sut.getSurnamesList(1)(
          using AuthenticatedRequest(FakeRequest(), Session("sessionId", SessionData(None), LocalDateTime.now))
        )
      } yield result).futureValue

      result mustBe List(
        ("A", 3, Some(100), Some(10000)),
        ("B", 1, None, None),
        ("C", 1, None, None),
        ("D", 1, None, None)
      )
    }

    "returns a list of names where restriction is not None" in {
      val result = (for {
        _ <- executeSql(
          sqlPersonDetails(fakePersonDetails(id = 1, surname = "D")) +
            sqlPersonDetails(fakePersonDetails(id = 2, surname = "B")) +
            sqlPersonDetails(fakePersonDetails(id = 3, surname = "C")) +
            sqlPersonDetails(fakePersonDetails(id = 4, surname = "A", privacyRestriction = Some(PrivacyResn)))
        )
        result <- sut.getSurnamesList(1)(
          using AuthenticatedRequest(
            FakeRequest(),
            Session("sessionId", SessionData(Some(UserData(0, "username", "hashed", true, false))), LocalDateTime.now)
          )
        )
      } yield result).futureValue

      result mustBe List(("A", 1, None, None), ("B", 1, None, None), ("C", 1, None, None), ("D", 1, None, None))
    }

    "returns nothing" in {
      val result = sut
        .getSurnamesList(1)(
          using AuthenticatedRequest(
            FakeRequest(),
            Session("sessionId", SessionData(Some(UserData(0, "username", "hashed", true, false))), LocalDateTime.now)
          )
        )
        .futureValue

      result mustBe List.empty
    }
  }

  "getFirstnamesList" must {
    "returns a list of names where restriction is None" in {
      val result = (for {
        _ <- executeSql(
          sqlPersonDetails(fakePersonDetails(id = 1, surname = "Z", firstname = "D")) +
            sqlPersonDetails(fakePersonDetails(id = 2, surname = "Z", firstname = "B")) +
            sqlPersonDetails(fakePersonDetails(id = 3, surname = "X", firstname = "C")) +
            sqlPersonDetails(
              fakePersonDetails(id = 4, surname = "Z", firstname = "A", privacyRestriction = Some(PrivacyResn))
            )
        )
        result <- sut.getAllPersonDetails(1, Some("Z"))(
          using AuthenticatedRequest(FakeRequest(), Session("sessionId", SessionData(None), LocalDateTime.now))
        )
      } yield result).futureValue

      result.map(_.shortName) mustBe List("B Z", "D Z")
    }

    "returns a list of names where restriction is not None" in {
      val result = (for {
        _ <- executeSql(
          sqlPersonDetails(fakePersonDetails(id = 1, surname = "Z", firstname = "D")) +
            sqlPersonDetails(fakePersonDetails(id = 2, surname = "Z", firstname = "B")) +
            sqlPersonDetails(fakePersonDetails(id = 3, surname = "X", firstname = "C")) +
            sqlPersonDetails(
              fakePersonDetails(id = 4, surname = "Z", firstname = "A", privacyRestriction = Some(PrivacyResn))
            )
        )
        result <- sut.getAllPersonDetails(1, Some("Z"))(
          using AuthenticatedRequest(
            FakeRequest(),
            Session("sessionId", SessionData(Some(UserData(0, "username", "hashed", true, false))), LocalDateTime.now)
          )
        )
      } yield result).futureValue

      result.map(_.shortName) mustBe List("A Z", "B Z", "D Z")
    }

    "returns nothing" in {
      val result = sut
        .getAllPersonDetails(1, Some("Z"))(
          using AuthenticatedRequest(
            FakeRequest(),
            Session("sessionId", SessionData(Some(UserData(0, "username", "hashed", true, false))), LocalDateTime.now)
          )
        )
        .futureValue

      result mustBe List.empty[PersonDetails]
    }
  }
}
