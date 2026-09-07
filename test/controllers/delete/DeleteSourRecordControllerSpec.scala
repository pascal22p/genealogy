package controllers.delete

import java.time.Instant
import java.time.LocalDateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import actions.AuthJourney
import cats.data.OptionT
import models.*
import org.jsoup.Jsoup
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.ActionBuilder
import play.api.mvc.AnyContent
import play.api.test.*
import play.api.test.CSRFTokenHelper.CSRFRequest
import play.api.test.Helpers.*
import queries.DeleteSqlQueries
import queries.GetSqlQueries
import services.GenealogyDatabaseService
import testUtils.BaseSpec
import testUtils.FakeAuthAction

class DeleteSourRecordControllerSpec extends BaseSpec {

  val userData: UserData                                     = UserData(1, "username", "hashedPassword", true, true)
  val fakeAuthAction: FakeAuthAction                         = new FakeAuthAction(Session("id", SessionData(Some(userData)), LocalDateTime.now))
  val mockGetSqlQueries: GetSqlQueries                       = mock[GetSqlQueries]
  val mockDeleteSqlQueries: DeleteSqlQueries                 = mock[DeleteSqlQueries]
  val mockGenealogyDatabaseService: GenealogyDatabaseService = mock[GenealogyDatabaseService]

  val authJourney: AuthJourney = new AuthJourney {
    override val authWithAdminRight: ActionBuilder[AuthenticatedRequest, AnyContent] =
      fakeAuthAction.andThen(new actions.AdminFilter(stubControllerComponents()))
  }

  protected override def localGuiceApplicationBuilder(): GuiceApplicationBuilder =
    GuiceApplicationBuilder()
      .overrides(
        bind[AuthJourney].toInstance(authJourney),
        bind[GetSqlQueries].toInstance(mockGetSqlQueries),
        bind[DeleteSqlQueries].toInstance(mockDeleteSqlQueries),
        bind[GenealogyDatabaseService].toInstance(mockGenealogyDatabaseService)
      )

  val sut: DeleteSourRecordController = app.injector.instanceOf[DeleteSourRecordController]

  val fakeSourRecord: SourRecord = SourRecord(
    id = 1,
    auth = "Author",
    title = "Title",
    abbr = "Abbreviation",
    publ = "Publication",
    agnc = "Agency",
    rin = "RIN123",
    repoId = Some(10),
    repoCaln = "CALN123",
    repoMedi = "Book",
    timestamp = Instant.now()
  )

  "deleteSourRecordConfirmation" must {
    "display confirmation screen when database and sourRecord exist" in {
      when(mockGenealogyDatabaseService.getGenealogyDatabase(1)).thenReturn(
        Future.successful(Some(GenealogyDatabase(1, "Name", "Description", None)))
      )
      when(mockGetSqlQueries.getSourRecord(1, 1)).thenReturn(
        OptionT.some[Future](fakeSourRecord)
      )

      val result = sut.deleteSourRecordConfirmation(1, 1).apply(FakeRequest().withCSRFToken)

      status(result) mustBe OK
      val html = Jsoup.parse(contentAsString(result))
      html.text() must include("Source record to be deleted")
      html.text() must include("Delete this source record")
      html.text() must include("Author")
      html.text() must include("Title")
    }

    "return NOT_FOUND when sourRecord does not exist" in {
      when(mockGenealogyDatabaseService.getGenealogyDatabase(1)).thenReturn(
        Future.successful(Some(GenealogyDatabase(1, "Name", "Description", None)))
      )
      when(mockGetSqlQueries.getSourRecord(1, 1)).thenReturn(
        OptionT.none[Future, SourRecord]
      )

      val result = sut.deleteSourRecordConfirmation(1, 1).apply(FakeRequest().withCSRFToken)

      status(result) mustBe NOT_FOUND
    }
  }

  "deleteSourRecordAction" must {
    "delete the sourRecord and redirect to sour records list" in {
      when(mockDeleteSqlQueries.deleteSourRecord(1, 1)).thenReturn(Future.successful(1))

      val result = sut.deleteSourRecordAction(1, 1).apply(FakeRequest().withCSRFToken)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.SourRecordsController.index(1, None).url)
      verify(mockDeleteSqlQueries).deleteSourRecord(1, 1)
    }
  }
}
