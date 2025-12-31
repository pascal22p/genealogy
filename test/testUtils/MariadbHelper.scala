package testUtils

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import anorm.SQL
import models.LoggingWithRequest
import org.scalatest.BeforeAndAfterEach
import play.api.db.Database
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.Application

trait MariadbHelper extends BaseSpec with BeforeAndAfterEach with LoggingWithRequest {
  lazy val db: Database                  = app.injector.instanceOf[Database]
  implicit lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  val testDataBase: String = "genealogie-test"

  implicit override lazy val app: Application = localGuiceApplicationBuilder()
    .configure(
      "database.name"  -> testDataBase,
      "db.default.url" -> "jdbc:mariadb://localhost:3306",
      "upload-path"    -> "test/resources/gedcom/"
    )
    .build()

  def executeSql(queries: String, logMe: Boolean = false)(implicit request: Request[?]): Future[Boolean] = Future {
    db.withConnection { implicit conn =>
      queries.trim
        .split(";")
        .map { query =>
          if (logMe) logger.error("Query: " + query)
          Try(SQL(query).execute()) match {
            case Success(bool)  => bool
            case Failure(error) =>
              logger.error("Error with query: " + query)
              throw error
          }
        }
        .reduce(_ && _)
    }
  }

  def createTables(implicit request: Request[?]): Future[Boolean] = {
    val source = scala.io.Source.fromFile("doc/tables.sql")
    val lines  =
      try source.mkString
      finally source.close()
    val queries =
      s"""DROP DATABASE IF EXISTS `$testDataBase`;
         |CREATE DATABASE `$testDataBase`;
         |USE `$testDataBase`;
         |""".stripMargin
        + lines
    executeSql(queries)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    createTables(using FakeRequest()).map(_ => ()).futureValue
  }

}
