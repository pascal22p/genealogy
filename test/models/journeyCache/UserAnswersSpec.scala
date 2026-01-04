package models.journeyCache

import java.time.LocalDateTime

import models.forms.CreateNewDatabaseForm
import models.forms.GedcomPathInputTextForm
import models.forms.PlacesElementsForm
import models.forms.PlacesElementsPaddingForm
import models.forms.PlacesElementsSeparatorForm
import models.forms.SelectExistingDatabaseForm
import models.forms.TrueOrFalseForm
import models.journeyCache.UserAnswersKey.*
import models.AuthenticatedRequest
import models.Session
import models.SessionData
import models.UserData
import play.api.i18n.Lang
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.i18n.MessagesImpl
import play.api.libs.json.Json
import play.api.libs.json.OFormat
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Call
import play.api.test.FakeRequest
import testUtils.BaseSpec

class UserAnswersSpec extends BaseSpec {

  implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] =
    AuthenticatedRequest(
      FakeRequest(),
      Session("", SessionData(Some(UserData(0, "", "", true, true))), LocalDateTime.now),
      None
    )

  val requiredKey: UserAnswersKey[GedcomPathInputTextForm] = ChooseGedcomFileQuestion
  val journey: JourneyId                                   = JourneyId.ImportGedcom

  case class TestItem(value: String) extends UserAnswersItem
  object TestItem {
    implicit val format: OFormat[TestItem] = Json.format[TestItem]
  }

  "UserAmswers.getItem" must {
    "get the item as instance of A" in {
      val record = CreateNewDatabaseForm("title", "description")

      val sut = UserAnswers(Map(NewDatabaseDetailsQuestion -> record))

      sut.getItem(NewDatabaseDetailsQuestion) mustBe record
    }

    "UserAnswers.getOptionalItem" should {
      "return Some when present" in {
        val ua = UserAnswers(Map(requiredKey -> GedcomPathInputTextForm("x")))

        ua.getOptionalItem(requiredKey).map(_.selectedFile) mustBe Some("x")
      }

      "return None when missing" in {
        val ua = UserAnswers(Map.empty)

        ua.getOptionalItem(requiredKey) mustBe None
      }
    }

    "UserAnswers.upsert" should {
      "insert or replace an item" in {
        val ua = UserAnswers(Map.empty)
          .upsert(requiredKey, GedcomPathInputTextForm("x"))
          .upsert(requiredKey, GedcomPathInputTextForm("y"))

        ua.getItem(requiredKey).selectedFile mustBe "y"
      }
    }

    "UserAnswers.validated" should {

      "redirect when a required item is missing" in {
        val ua = UserAnswers(Map.empty)

        ua.validated(journey) mustBe Left(Call("GET", "/list"))
      }

      "keep required items and removed optional ones when valid" in {
        val ua = UserAnswers(
          Map(
            ChooseGedcomFileQuestion        -> GedcomPathInputTextForm("x"),
            PlacesElementsQuestion          -> PlacesElementsForm(List.empty),
            CreateNewDatabaseQuestion       -> TrueOrFalseForm(false),
            ClearExistingDatabaseQuestion   -> TrueOrFalseForm(true),
            SelectExistingDatabaseQuestion  -> SelectExistingDatabaseForm(0, "title"),
            NewDatabaseDetailsQuestion      -> CreateNewDatabaseForm("name", "desc"),
            PlacesElementsSeparatorQuestion -> PlacesElementsSeparatorForm(","),
            PlacesElementsPaddingQuestion   -> PlacesElementsPaddingForm("left")
          )
        )

        val expected = Set(
          ChooseGedcomFileQuestion,
          PlacesElementsQuestion,
          PlacesElementsSeparatorQuestion,
          PlacesElementsPaddingQuestion,
          CreateNewDatabaseQuestion,
          ClearExistingDatabaseQuestion,
          SelectExistingDatabaseQuestion
        )

        ua.validated(journey).map(_.data.keySet) mustBe
          Right(expected)
      }
    }

    "UserAnswers.flattenByKey" should {
      "flatten simple values" in {
        val ua = UserAnswers(
          Map(requiredKey -> GedcomPathInputTextForm("hello"))
        )

        implicit def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
        implicit lazy val messages: Messages  = MessagesImpl(Lang("en"), messagesApi)

        val result = ua.flattenByKey(journey)

        result(requiredKey) mustBe Map("selectedFile" -> "hello")
      }
    }
  }
}
