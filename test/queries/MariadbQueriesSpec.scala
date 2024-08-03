package queries

import java.time.Instant

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import anorm.*
import cats.implicits.*
import cats.syntax.*
import models.PersonDetails
import org.scalatest.matchers.should.Matchers.should
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.*
import play.api.db.Database
import play.api.i18n.Lang
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.i18n.MessagesImpl
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

  def executeSql(queries: String): Future[Boolean] = Future {
    db.withConnection { implicit conn =>
      queries.trim
        .split(";")
        .map { query =>
          SQL(query).execute()
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

  def sqlPersonDetails(id: Int): String =
    s"""INSERT INTO `genea_individuals` (`indi_id`, `base`, `indi_nom`, `indi_prenom`, `indi_sexe`, `indi_npfx`, `indi_givn`, `indi_nick`, `indi_spfx`, `indi_nsfx`, `indi_resn`) VALUES
       |($id,	1,	'PAROIS',	'Alphonse Auguste Joseph Marie',	'M',	'',	'',	'',	'',	'',	NULL);
       |""".stripMargin

  "getPersonDetails" must {
    "returns person details" in {
      val idPerson = 1
      val result = (for {
        _      <- executeSql(sqlPersonDetails(idPerson))
        result <- sut.getPersonDetails(idPerson)
      } yield result).futureValue

      result mustBe a[List[PersonDetails]]
      result.size mustBe 1
      result.head.id mustBe idPerson
    }

    "returns nothing" in {
      val idPerson = 1
      val result   = sut.getPersonDetails(idPerson).futureValue

      result mustBe List.empty
    }
  }

}
