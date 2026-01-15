package services

import java.time.LocalDateTime

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*
import scala.language.postfixOps

import config.AppConfig
import models.queryData.FirstnameWithBirthDeath
import models.AuthenticatedRequest
import models.Cursor
import models.FirstnamesListPagination
import models.Session
import models.SessionData
import org.mockito.ArgumentCaptor
import queries.GetSqlQueries
import testUtils.BaseSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import play.api.i18n.Lang
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.i18n.MessagesImpl
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.await
import play.api.test.Helpers.defaultAwaitTimeout

class FirstnamesListServiceSpec extends BaseSpec {

  val mockAppConfig: AppConfig         = mock[AppConfig]
  val mockGetSqlQueries: GetSqlQueries = mock[GetSqlQueries]

  val request                                                                    = FakeRequest()
  implicit val authenticatedRquest: AuthenticatedRequest[AnyContentAsEmpty.type] =
    AuthenticatedRequest(request, Session("sessionId", SessionData(None), LocalDateTime.now), None)
  implicit def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messages: Messages  = MessagesImpl(Lang("en"), messagesApi)

  val sut = new FirstnamesListService(mockGetSqlQueries, mockAppConfig)(using global)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAppConfig, mockGetSqlQueries)
    when(mockAppConfig.pageSize).thenReturn(1)
    ()
  }

  "getFirstNamesListWithAnchors" must {
    "return no anchor" in {
      when(mockGetSqlQueries.getFirstNamesList(eqTo(1), any(), any(), eqTo(None), eqTo(false))(using any())).thenReturn(
        Future.successful(Seq.empty)
      )
      when(mockGetSqlQueries.getFirstNamesList(eqTo(1), any(), any(), any(), eqTo(true))(using any())).thenReturn(
        Future.successful(Seq.empty)
      )
      val argumentCaptorCursor: ArgumentCaptor[Option[(String, Int, Int, Int)]] =
        ArgumentCaptor.forClass(classOf[Option[(String, Int, Int, Int)]])

      val result = await(sut.getFirstNamesListWithAnchors(1, "test", None))

      result mustBe FirstnamesListPagination(Seq.empty, None, Seq.empty, Seq.empty, None, None)
      verify(mockGetSqlQueries, times(1)).getFirstNamesList(
        eqTo(1),
        any(),
        any(),
        argumentCaptorCursor.capture(),
        eqTo(false)
      )(using any())
      val capturedCursor = argumentCaptorCursor.getAllValues.asScala.toList
      capturedCursor mustBe List(None)
    }

    "return one current anchor" in {
      val currentCursorTuple = ("M", 1, 0, 0)
      val currentCursor      = Cursor.apply.tupled(currentCursorTuple :* "M")
      val data               = Seq(FirstnameWithBirthDeath(1, "M", None, None, 0, 0))
      when(
        mockGetSqlQueries.getFirstNamesList(eqTo(1), any(), any(), eqTo(Some(currentCursorTuple)), eqTo(false))(
          using any()
        )
      )
        .thenReturn(
          Future.successful(data)
        )
      when(mockGetSqlQueries.getFirstNamesList(eqTo(1), any(), any(), any(), eqTo(true))(using any())).thenReturn(
        Future.successful(Seq.empty)
      )
      val argumentCaptorCursor: ArgumentCaptor[Option[(String, Int, Int, Int)]] =
        ArgumentCaptor.forClass(classOf[Option[(String, Int, Int, Int)]])

      val result = await(sut.getFirstNamesListWithAnchors(1, "test", Some(currentCursorTuple)))

      result mustBe FirstnamesListPagination(data, Some(currentCursor), Seq.empty, Seq.empty, None, None)
      verify(mockGetSqlQueries, times(1)).getFirstNamesList(
        eqTo(1),
        any(),
        any(),
        argumentCaptorCursor.capture(),
        eqTo(true)
      )(using any())
      val capturedCursor = argumentCaptorCursor.getAllValues.asScala.toList
      capturedCursor mustBe List(Some(currentCursorTuple))
    }

    "return 6 anchors in next" in {
      // last is not used because all the pages fits in the navigation
      // one more page than needed is always fetch to check if the last page is just a normal page or a next link

      val currentCursorTuple = ("M", 1, 0, 0)
      val currentCursor      = Cursor.apply.tupled(currentCursorTuple :* "M")
      val data               = Seq(
        FirstnameWithBirthDeath(1, "M", None, None, 0, 0),
        FirstnameWithBirthDeath(2, "N", None, None, 0, 0),
        FirstnameWithBirthDeath(3, "O", None, None, 0, 0),
        FirstnameWithBirthDeath(4, "P", None, None, 0, 0),
        FirstnameWithBirthDeath(5, "Q", None, None, 0, 0),
        FirstnameWithBirthDeath(6, "R", None, None, 0, 0),
        FirstnameWithBirthDeath(7, "S", None, None, 0, 0)
      )
      when(
        mockGetSqlQueries.getFirstNamesList(eqTo(1), any(), any(), eqTo(Some(currentCursorTuple)), eqTo(false))(
          using any()
        )
      )
        .thenReturn(
          Future.successful(data)
        )
      when(mockGetSqlQueries.getFirstNamesList(eqTo(1), any(), any(), any(), eqTo(true))(using any())).thenReturn(
        Future.successful(Seq.empty)
      )
      val argumentCaptorCursor: ArgumentCaptor[Option[(String, Int, Int, Int)]] =
        ArgumentCaptor.forClass(classOf[Option[(String, Int, Int, Int)]])

      val result = await(sut.getFirstNamesListWithAnchors(1, "test", Some(currentCursorTuple)))

      result mustBe FirstnamesListPagination(
        data.take(1),
        Some(currentCursor),
        Seq.empty,
        data.drop(1).map(d => Cursor(d.firstname, d.id, 0, 0, d.firstname)),
        None,
        None
      )
      verify(mockGetSqlQueries, times(1)).getFirstNamesList(
        eqTo(1),
        any(),
        any(),
        argumentCaptorCursor.capture(),
        eqTo(true)
      )(using any())
      val capturedCursor = argumentCaptorCursor.getAllValues.asScala.toList
      capturedCursor mustBe List(Some(currentCursorTuple))
    }

    "return 5 anchors in next + last" in {
      // last is used because all the pages don't fits in the navigation
      // one more page than needed is always fetch to check if the last page is just a normal page or a next link

      val currentCursorTuple = ("M", 1, 0, 0)
      val currentCursor      = Cursor.apply.tupled(currentCursorTuple :* "M")
      val data               = Seq(
        FirstnameWithBirthDeath(1, "M", None, None, 0, 0),
        FirstnameWithBirthDeath(2, "N", None, None, 0, 0),
        FirstnameWithBirthDeath(3, "O", None, None, 0, 0),
        FirstnameWithBirthDeath(4, "P", None, None, 0, 0),
        FirstnameWithBirthDeath(5, "Q", None, None, 0, 0),
        FirstnameWithBirthDeath(6, "R", None, None, 0, 0),
        FirstnameWithBirthDeath(7, "S", None, None, 0, 0),
        FirstnameWithBirthDeath(8, "T", None, None, 0, 0)
      )
      when(
        mockGetSqlQueries.getFirstNamesList(eqTo(1), any(), any(), eqTo(Some(currentCursorTuple)), eqTo(false))(
          using any()
        )
      )
        .thenReturn(
          Future.successful(data)
        )
      when(mockGetSqlQueries.getFirstNamesList(eqTo(1), any(), any(), any(), eqTo(true))(using any())).thenReturn(
        Future.successful(Seq.empty)
      )
      val argumentCaptorCursor: ArgumentCaptor[Option[(String, Int, Int, Int)]] =
        ArgumentCaptor.forClass(classOf[Option[(String, Int, Int, Int)]])

      val result = await(sut.getFirstNamesListWithAnchors(1, "test", Some(currentCursorTuple)))

      result mustBe FirstnamesListPagination(
        data.take(1),
        Some(currentCursor),
        Seq.empty,
        data.drop(1).dropRight(2).map(d => Cursor(d.firstname, d.id, 0, 0, d.firstname)),
        None,
        data.dropRight(1).lastOption.map(d => Cursor(d.firstname, d.id, 0, 0, d.firstname))
      )
      verify(mockGetSqlQueries, times(1)).getFirstNamesList(
        eqTo(1),
        any(),
        any(),
        argumentCaptorCursor.capture(),
        eqTo(true)
      )(using any())
      val capturedCursor = argumentCaptorCursor.getAllValues.asScala.toList
      capturedCursor mustBe List(Some(currentCursorTuple))
    }

    "return 1 anchor in previous + 4 anchors in next + last" in {
      // last is used because all the pages don't fit in the navigation
      // one more page than needed is always fetch to check if the last page is just a normal page or a next link

      val currentCursorTuple = ("M", 1, 0, 0)
      val currentCursor      = Cursor.apply.tupled(currentCursorTuple :* "M")
      val dataPrevious       = Seq(
        FirstnameWithBirthDeath(9, "L", None, None, 0, 0)
      )
      val dataNext = Seq(
        FirstnameWithBirthDeath(1, "M", None, None, 0, 0),
        FirstnameWithBirthDeath(2, "N", None, None, 0, 0),
        FirstnameWithBirthDeath(3, "O", None, None, 0, 0),
        FirstnameWithBirthDeath(4, "P", None, None, 0, 0),
        FirstnameWithBirthDeath(5, "Q", None, None, 0, 0),
        FirstnameWithBirthDeath(6, "R", None, None, 0, 0),
        FirstnameWithBirthDeath(7, "S", None, None, 0, 0),
        FirstnameWithBirthDeath(8, "T", None, None, 0, 0)
      )
      when(
        mockGetSqlQueries.getFirstNamesList(eqTo(1), any(), any(), eqTo(Some(currentCursorTuple)), eqTo(false))(
          using any()
        )
      )
        .thenReturn(
          Future.successful(dataNext)
        )
      when(mockGetSqlQueries.getFirstNamesList(eqTo(1), any(), any(), any(), eqTo(true))(using any())).thenReturn(
        Future.successful(dataPrevious)
      )
      val argumentCaptorCursor: ArgumentCaptor[Option[(String, Int, Int, Int)]] =
        ArgumentCaptor.forClass(classOf[Option[(String, Int, Int, Int)]])

      val result = await(sut.getFirstNamesListWithAnchors(1, "test", Some(currentCursorTuple)))

      result mustBe FirstnamesListPagination(
        dataNext.take(1),
        Some(currentCursor),
        dataPrevious.map(d => Cursor(d.firstname, d.id, 0, 0, d.firstname)),
        dataNext.drop(1).dropRight(3).map(d => Cursor(d.firstname, d.id, 0, 0, d.firstname)),
        None,
        dataNext.dropRight(2).lastOption.map(d => Cursor(d.firstname, d.id, 0, 0, d.firstname))
      )
      verify(mockGetSqlQueries, times(1)).getFirstNamesList(
        eqTo(1),
        any(),
        any(),
        argumentCaptorCursor.capture(),
        eqTo(true)
      )(using any())
      val capturedCursor = argumentCaptorCursor.getAllValues.asScala.toList
      capturedCursor mustBe List(Some(currentCursorTuple))
    }

    "return 2 anchors in previous + 2 anchors in next + first and last" in {
      // first and last are used because all the pages don't fit in the navigation
      // one more page than needed is always fetch to check if the last page is just a normal page or a next link

      val currentCursorTuple = ("M", 1, 0, 0)
      val dataPrevious       = Seq(
        FirstnameWithBirthDeath(15, "L", None, None, 0, 0),
        FirstnameWithBirthDeath(14, "K", None, None, 0, 0),
        FirstnameWithBirthDeath(13, "J", None, None, 0, 0),
        FirstnameWithBirthDeath(12, "I", None, None, 0, 0),
        FirstnameWithBirthDeath(11, "H", None, None, 0, 0),
        FirstnameWithBirthDeath(10, "G", None, None, 0, 0),
        FirstnameWithBirthDeath(9, "F", None, None, 0, 0),
      )
      val currentCursor = Cursor.apply.tupled(currentCursorTuple :* "M")
      val dataNext      = Seq(
        FirstnameWithBirthDeath(1, "M", None, None, 0, 0),
        FirstnameWithBirthDeath(2, "N", None, None, 0, 0),
        FirstnameWithBirthDeath(3, "O", None, None, 0, 0),
        FirstnameWithBirthDeath(4, "P", None, None, 0, 0),
        FirstnameWithBirthDeath(5, "Q", None, None, 0, 0),
        FirstnameWithBirthDeath(6, "R", None, None, 0, 0),
        FirstnameWithBirthDeath(7, "S", None, None, 0, 0),
        FirstnameWithBirthDeath(8, "T", None, None, 0, 0)
      )
      when(
        mockGetSqlQueries.getFirstNamesList(eqTo(1), any(), any(), eqTo(Some(currentCursorTuple)), eqTo(false))(
          using any()
        )
      )
        .thenReturn(
          Future.successful(dataNext)
        )
      when(mockGetSqlQueries.getFirstNamesList(eqTo(1), any(), any(), any(), eqTo(true))(using any())).thenReturn(
        Future.successful(dataPrevious)
      )
      val argumentCaptorCursor: ArgumentCaptor[Option[(String, Int, Int, Int)]] =
        ArgumentCaptor.forClass(classOf[Option[(String, Int, Int, Int)]])

      val result = await(sut.getFirstNamesListWithAnchors(1, "test", Some(currentCursorTuple)))

      result mustBe FirstnamesListPagination(
        dataNext.take(1),
        Some(currentCursor),
        dataPrevious.take(2).reverse.map(d => Cursor(d.firstname, d.id, 0, 0, d.firstname)),
        dataNext.slice(1, 3).map(d => Cursor(d.firstname, d.id, 0, 0, d.firstname)),
        Some(Cursor("J", 13, 0, 0, "J")),
        dataNext.drop(3).headOption.map(d => Cursor(d.firstname, d.id, 0, 0, d.firstname))
      )
      verify(mockGetSqlQueries, times(1)).getFirstNamesList(
        eqTo(1),
        any(),
        any(),
        argumentCaptorCursor.capture(),
        eqTo(true)
      )(using any())
      val capturedCursor = argumentCaptorCursor.getAllValues.asScala.toList
      capturedCursor mustBe List(Some(currentCursorTuple))
    }

    "return 3 anchors in previous + 3 anchors in next only" in {
      // first and last are not used because there is exactly 7 pages
      // one more page than needed is always fetch to check if the last page is just a normal page or a next link

      val currentCursorTuple = ("M", 1, 0, 0)
      val dataPrevious       = Seq(
        FirstnameWithBirthDeath(15, "L", None, None, 0, 0),
        FirstnameWithBirthDeath(14, "K", None, None, 0, 0),
        FirstnameWithBirthDeath(13, "J", None, None, 0, 0)
      )
      val currentCursor = Cursor.apply.tupled(currentCursorTuple :* "M")
      val dataNext      = Seq(
        FirstnameWithBirthDeath(1, "M", None, None, 0, 0),
        FirstnameWithBirthDeath(2, "N", None, None, 0, 0),
        FirstnameWithBirthDeath(3, "O", None, None, 0, 0),
        FirstnameWithBirthDeath(4, "P", None, None, 0, 0)
      )
      when(
        mockGetSqlQueries.getFirstNamesList(eqTo(1), any(), any(), eqTo(Some(currentCursorTuple)), eqTo(false))(
          using any()
        )
      )
        .thenReturn(
          Future.successful(dataNext)
        )
      when(mockGetSqlQueries.getFirstNamesList(eqTo(1), any(), any(), any(), eqTo(true))(using any())).thenReturn(
        Future.successful(dataPrevious)
      )
      val argumentCaptorCursor: ArgumentCaptor[Option[(String, Int, Int, Int)]] =
        ArgumentCaptor.forClass(classOf[Option[(String, Int, Int, Int)]])

      val result = await(sut.getFirstNamesListWithAnchors(1, "test", Some(currentCursorTuple)))

      result mustBe FirstnamesListPagination(
        dataNext.take(1),
        Some(currentCursor),
        dataPrevious.take(3).reverse.map(d => Cursor(d.firstname, d.id, 0, 0, d.firstname)),
        dataNext.slice(1, 4).map(d => Cursor(d.firstname, d.id, 0, 0, d.firstname)),
        None,
        None
      )
      verify(mockGetSqlQueries, times(1)).getFirstNamesList(
        eqTo(1),
        any(),
        any(),
        argumentCaptorCursor.capture(),
        eqTo(true)
      )(using any())
      val capturedCursor = argumentCaptorCursor.getAllValues.asScala.toList
      capturedCursor mustBe List(Some(currentCursorTuple))
    }

    "return 5 anchors in previous + first" in {
      // last is used because all the pages don't fit in the navigation
      // one more page than needed is always fetch to check if the last page is just a normal page or a next link

      val currentCursorTuple = ("M", 1, 0, 0)
      val currentCursor      = Cursor.apply.tupled(currentCursorTuple :* "M")
      val dataPrevious       = Seq(
        FirstnameWithBirthDeath(9, "L", None, None, 0, 0),
        FirstnameWithBirthDeath(8, "K", None, None, 0, 0),
        FirstnameWithBirthDeath(7, "J", None, None, 0, 0),
        FirstnameWithBirthDeath(6, "I", None, None, 0, 0),
        FirstnameWithBirthDeath(5, "H", None, None, 0, 0),
        FirstnameWithBirthDeath(4, "G", None, None, 0, 0),
        FirstnameWithBirthDeath(3, "F", None, None, 0, 0),
        FirstnameWithBirthDeath(2, "E", None, None, 0, 0),
      )
      val dataNext = Seq(
        FirstnameWithBirthDeath(1, "M", None, None, 0, 0)
      )
      when(
        mockGetSqlQueries.getFirstNamesList(eqTo(1), any(), any(), eqTo(Some(currentCursorTuple)), eqTo(false))(
          using any()
        )
      )
        .thenReturn(
          Future.successful(dataNext)
        )
      when(mockGetSqlQueries.getFirstNamesList(eqTo(1), any(), any(), any(), eqTo(true))(using any())).thenReturn(
        Future.successful(dataPrevious)
      )
      val argumentCaptorCursor: ArgumentCaptor[Option[(String, Int, Int, Int)]] =
        ArgumentCaptor.forClass(classOf[Option[(String, Int, Int, Int)]])

      val result = await(sut.getFirstNamesListWithAnchors(1, "test", Some(currentCursorTuple)))

      result mustBe FirstnamesListPagination(
        dataNext.take(1),
        Some(currentCursor),
        dataPrevious.take(5).reverse.map(d => Cursor(d.firstname, d.id, 0, 0, d.firstname)),
        Seq.empty,
        Some(Cursor("G", 4, 0, 0, "G")),
        None
      )
      verify(mockGetSqlQueries, times(1)).getFirstNamesList(
        eqTo(1),
        any(),
        any(),
        argumentCaptorCursor.capture(),
        eqTo(true)
      )(using any())
      val capturedCursor = argumentCaptorCursor.getAllValues.asScala.toList
      capturedCursor mustBe List(Some(currentCursorTuple))
    }

    "return 4 anchors in previous + 1 anchor in next + first" in {
      // last is used because all the pages don't fit in the navigation
      // one more page than needed is always fetch to check if the last page is just a normal page or a next link

      val currentCursorTuple = ("M", 1, 0, 0)
      val currentCursor      = Cursor.apply.tupled(currentCursorTuple :* "M")
      val dataPrevious       = Seq(
        FirstnameWithBirthDeath(9, "L", None, None, 0, 0),
        FirstnameWithBirthDeath(8, "K", None, None, 0, 0),
        FirstnameWithBirthDeath(7, "J", None, None, 0, 0),
        FirstnameWithBirthDeath(6, "I", None, None, 0, 0),
        FirstnameWithBirthDeath(5, "H", None, None, 0, 0),
        FirstnameWithBirthDeath(4, "G", None, None, 0, 0),
        FirstnameWithBirthDeath(3, "F", None, None, 0, 0),
        FirstnameWithBirthDeath(2, "E", None, None, 0, 0),
      )
      val dataNext = Seq(
        FirstnameWithBirthDeath(1, "M", None, None, 0, 0),
        FirstnameWithBirthDeath(10, "N", None, None, 0, 0)
      )
      when(
        mockGetSqlQueries.getFirstNamesList(eqTo(1), any(), any(), eqTo(Some(currentCursorTuple)), eqTo(false))(
          using any()
        )
      )
        .thenReturn(
          Future.successful(dataNext)
        )
      when(mockGetSqlQueries.getFirstNamesList(eqTo(1), any(), any(), any(), eqTo(true))(using any())).thenReturn(
        Future.successful(dataPrevious)
      )
      val argumentCaptorCursor: ArgumentCaptor[Option[(String, Int, Int, Int)]] =
        ArgumentCaptor.forClass(classOf[Option[(String, Int, Int, Int)]])

      val result = await(sut.getFirstNamesListWithAnchors(1, "test", Some(currentCursorTuple)))

      result mustBe FirstnamesListPagination(
        dataNext.take(1),
        Some(currentCursor),
        dataPrevious.take(4).reverse.map(d => Cursor(d.firstname, d.id, 0, 0, d.firstname)),
        dataNext.slice(1, 2).map(d => Cursor(d.firstname, d.id, 0, 0, d.firstname)),
        Some(Cursor("H", 5, 0, 0, "H")),
        None
      )
      verify(mockGetSqlQueries, times(1)).getFirstNamesList(
        eqTo(1),
        any(),
        any(),
        argumentCaptorCursor.capture(),
        eqTo(true)
      )(using any())
      val capturedCursor = argumentCaptorCursor.getAllValues.asScala.toList
      capturedCursor mustBe List(Some(currentCursorTuple))
    }

    "return a single page with multiple elements" in {
      when(mockAppConfig.pageSize).thenReturn(10)

      val currentCursorTuple = ("M", 1, 0, 0)
      val currentCursor      = Cursor.apply.tupled(currentCursorTuple :* "M")
      val dataPrevious       = Seq.empty
      val dataNext           = Seq(
        FirstnameWithBirthDeath(1, "M", None, None, 0, 0),
        FirstnameWithBirthDeath(2, "N", None, None, 0, 0),
        FirstnameWithBirthDeath(3, "O", None, None, 0, 0),
        FirstnameWithBirthDeath(4, "P", None, None, 0, 0),
        FirstnameWithBirthDeath(5, "Q", None, None, 0, 0),
      )
      when(
        mockGetSqlQueries.getFirstNamesList(eqTo(1), any(), any(), eqTo(Some(currentCursorTuple)), eqTo(false))(
          using any()
        )
      )
        .thenReturn(
          Future.successful(dataNext)
        )
      when(mockGetSqlQueries.getFirstNamesList(eqTo(1), any(), any(), any(), eqTo(true))(using any())).thenReturn(
        Future.successful(dataPrevious)
      )
      val argumentCaptorCursor: ArgumentCaptor[Option[(String, Int, Int, Int)]] =
        ArgumentCaptor.forClass(classOf[Option[(String, Int, Int, Int)]])

      val result = await(sut.getFirstNamesListWithAnchors(1, "test", Some(currentCursorTuple)))

      result mustBe FirstnamesListPagination(
        dataNext,
        Some(currentCursor),
        Seq.empty,
        Seq.empty,
        None,
        None
      )
      verify(mockGetSqlQueries, times(1)).getFirstNamesList(
        eqTo(1),
        any(),
        any(),
        argumentCaptorCursor.capture(),
        eqTo(true)
      )(using any())
      val capturedCursor = argumentCaptorCursor.getAllValues.asScala.toList
      capturedCursor mustBe List(Some(currentCursorTuple))
    }

  }
}
