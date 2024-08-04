package queries

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import anorm.execute
import anorm.SQL
import models.*
import models.queryData.EventDetailQueryData
import models.queryData.FamilyAsChildQueryData
import models.queryData.FamilyQueryData
import org.scalatest.BeforeAndAfterEach
import play.api.db.Database
import play.api.Application
import testUtils.BaseSpec

class MariadbQueriesSpec extends BaseSpec with BeforeAndAfterEach {

  lazy val db: Database                  = app.injector.instanceOf[Database]
  implicit lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  lazy val sut: MariadbQueries           = app.injector.instanceOf[MariadbQueries]

  val testDataBase: String = "genealogie-test"

  implicit override lazy val app: Application = localGuiceApplicationBuilder()
    .configure(
      "db.default.url" -> "jdbc:mariadb://localhost:3306"
    )
    .build()

  def executeSql(queries: String, logMe: Boolean = false): Future[Boolean] = Future {
    db.withConnection { implicit conn =>
      queries.trim
        .split(";")
        .map { query =>
          if (logMe) println("Query: " + query)
          Try(SQL(query).execute()) match {
            case Success(bool) => bool
            case Failure(error) =>
              println("Error with query: " + query)
              throw error
          }
        }
        .reduce(_ && _)
    }
  }

  def createTables(): Future[Boolean] = {
    val source = scala.io.Source.fromFile("doc/tables.sql")
    val lines =
      try source.mkString
      finally source.close()
    val queries =
      s"""DROP DATABASE IF EXISTS `$testDataBase`;
         |CREATE DATABASE `$testDataBase`;
         |USE `$testDataBase`;
         |""".stripMargin + lines
    executeSql(queries)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    createTables().futureValue
  }

  def sqlPersonDetails(person: PersonDetails): String =
    s"""INSERT INTO `genea_individuals` (`indi_id`, `base`, `indi_nom`, `indi_prenom`, `indi_sexe`, `indi_npfx`, `indi_givn`, `indi_nick`, `indi_spfx`, `indi_nsfx`, `indi_resn`) VALUES
       |(${person.id},	${person.base},	'${person.surname}',	'${person.firstname}',	'${person.sex.gedcom}',	'${person.firstnamePrefix}',	'${person.nameGiven}',	'${person.nameNickname}',	'${person.surnamePrefix}',	'${person.nameSuffix}',	${person.privacyRestriction.fold("NULL")(r => s"'$r'")});
       |""".stripMargin

  def sqlEventDetails(event: EventDetail): String =
    s"""INSERT INTO `genea_events_details` (`events_details_id`, `place_id`, `addr_id`, `events_details_descriptor`, `events_details_gedcom_date`, `events_details_age`, `events_details_cause`, `jd_count`, `jd_precision`, `jd_calendar`, `events_details_famc`, `events_details_adop`, `base`) VALUES
       |(${event.events_details_id},	1,	NULL,	'',	'15 MAY 1835',	'',	'',	2391414,	3,	'@#DGREGORIAN@',	NULL,	NULL,	1);
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

  def sqlLinkFamilyEvent(idFamily: Int, event: EventDetail): String =
    s"""
       |INSERT INTO `rel_familles_events` (`events_details_id`, `familles_id`, `events_tag`, `events_attestation`) VALUES
       |(${event.events_details_id},	$idFamily,	'${event.tag}',	NULL);
       |""".stripMargin

  def sqlChild(child: Child, idFamily: Int): String =
    sqlPersonDetails(child.person.details) +
      s"""
         |INSERT INTO `rel_familles_indi` (`indi_id`, `familles_id`, `rela_type`) VALUES
         |(${child.person.details.id},	$idFamily, '${child.relaType}');
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

      result mustBe List.empty
    }
  }

  "getIndividualEvents" must {
    "returns individual events" in {
      val person   = fakePersonDetails(id = 1)
      val event    = fakeEventDetail(events_details_id = 2)
      val eventTag = "BIRT"
      val result = (for {
        _      <- executeSql(sqlIndividualEvent(person, event, eventTag))
        result <- sut.getIndividualEvents(person.id)
      } yield result).futureValue

      result mustBe a[List[EventDetailQueryData]]
      result.size mustBe 1
      result.head.events_details_id mustBe event.events_details_id
      result.head.tag mustBe eventTag
    }

    "returns nothing" in {
      val idPerson = 1
      val result   = sut.getIndividualEvents(idPerson).futureValue

      result mustBe List.empty
    }
  }

  "getFamilyEvents" must {
    "returns family events" in {
      val person1  = fakePersonDetails(id = 1)
      val person2  = fakePersonDetails(id = 2)
      val idFamily = 3
      val event    = fakeEventDetail(events_details_id = 4, tag = "BIRT")
      val result = (for {
        _      <- executeSql(sqlFamily(idFamily, person1, person2, List.empty, List(event)))
        result <- sut.getFamilyEvents(idFamily)
      } yield result).futureValue

      result mustBe a[List[EventDetailQueryData]]
      result.size mustBe 1
      result.head.events_details_id mustBe event.events_details_id
      result.head.tag mustBe event.tag
    }

    "returns nothing" in {
      val idPerson = 1
      val result   = sut.getFamilyEvents(idPerson).futureValue

      result mustBe List.empty
    }
  }

  "getFamiliesFromIndividualId" must {
    "returns families" in {
      val person1  = fakePersonDetails(id = 1)
      val person2  = fakePersonDetails(id = 2)
      val idFamily = 3
      val child    = Child(Person(fakePersonDetails(id = 5), Events(List.empty)), "adopted", None)
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

      result mustBe List.empty
    }
  }

  "getFamiliesAsPartner" must {
    "returns family when person is husb" in {
      val person1  = fakePersonDetails(id = 1)
      val person2  = fakePersonDetails(id = 2)
      val idFamily = 3
      val result = (for {
        _      <- executeSql(sqlFamily(idFamily, person1, person2, List.empty, List.empty))
        result <- sut.getFamiliesAsPartner(person1.id)
      } yield result).futureValue

      result mustBe a[List[FamilyQueryData]]
      result.size mustBe 1
      result.head.parent1 mustBe Some(person1.id)
      result.head.parent2 mustBe Some(person2.id)
    }

    "returns family when person is wife" in {
      val person1  = fakePersonDetails(id = 1)
      val person2  = fakePersonDetails(id = 2)
      val idFamily = 3
      val result = (for {
        _      <- executeSql(sqlFamily(idFamily, person1, person2, List.empty, List.empty))
        result <- sut.getFamiliesAsPartner(person2.id)
      } yield result).futureValue

      result mustBe a[List[FamilyQueryData]]
      result.size mustBe 1
      result.head.parent1 mustBe Some(person1.id)
      result.head.parent2 mustBe Some(person2.id)
    }

    "returns nothing" in {
      val idPerson = 1
      val result   = sut.getFamiliesAsPartner(idPerson).futureValue

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
}
