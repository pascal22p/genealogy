package models.journeyCache

import models.forms.*
import play.api.i18n.Messages
import play.api.libs.json.*
import play.api.mvc.Call

enum UserAnswersKey[A <: UserAnswersItem](
    val page: Call,
    val requirement: ItemRequirements,
    val journeyId: JourneyId,
    val index: Int = 0,
    val checkYourAnswerWrites: Option[Messages => OWrites[A]] = None // optional format for check your answers page.
)(using val format: OFormat[A]) { // format used to serialize/deserialize A. Also used as default if checkYourAnswerFormat is None.
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  def writeUserAnswersItemAsJson(value: UserAnswersItem): JsValue =
    format.writes(value.asInstanceOf[A])

  def readUserAnswersItemFromJson(json: JsValue): JsResult[UserAnswersItem] =
    format.reads(json)

  case ChooseGedcomFileQuestion
      extends UserAnswersKey[GedcomPathInputTextForm](
        page = controllers.gedcom.routes.ImportGedcomController.showGedcomList,
        requirement = ItemRequirements.Always(),
        journeyId = JourneyId.ImportGedcom,
        index = 1,
      )(using Json.format[GedcomPathInputTextForm])

  case PlacesElementsQuestion
      extends UserAnswersKey[PlacesElementsForm](
        page = controllers.gedcom.routes.GedcomPlaceParserController.chooseExtractPlaceElements,
        requirement = ItemRequirements.Always(),
        journeyId = JourneyId.ImportGedcom,
        index = 4,
        checkYourAnswerWrites = Some(messages => PlacesElementsForm.cyaWrites(using messages))
      )(using Json.format[PlacesElementsForm])

  case PlacesElementsSeparatorQuestion
      extends UserAnswersKey[PlacesElementsSeparatorForm](
        page = controllers.gedcom.routes.ChoosePlaceSeparatorController.showForm,
        requirement = ItemRequirements.Always(),
        journeyId = JourneyId.ImportGedcom,
        index = 2,
      )(using Json.format[PlacesElementsSeparatorForm])

  case PlacesElementsPaddingQuestion
      extends UserAnswersKey[PlacesElementsPaddingForm](
        page = controllers.gedcom.routes.ChoosePlacePaddingController.showForm,
        requirement = ItemRequirements.Always(),
        journeyId = JourneyId.ImportGedcom,
        index = 3,
        checkYourAnswerWrites = Some(messages => PlacesElementsPaddingForm.cyaWrites(using messages))
      )(using Json.format[PlacesElementsPaddingForm])

  case CreateNewDatabaseQuestion
      extends UserAnswersKey[TrueOrFalseForm](
        page = controllers.gedcom.routes.ImportGedcomController.showNewDatabaseQuestion,
        requirement = ItemRequirements.Always(),
        journeyId = JourneyId.ImportGedcom,
        index = 5,
        checkYourAnswerWrites = Some(messages => TrueOrFalseForm.cyaWrites(using messages))
      )(using Json.format[TrueOrFalseForm])

  case ClearExistingDatabaseQuestion
      extends UserAnswersKey[TrueOrFalseForm](
        page = controllers.gedcom.routes.ClearDatabaseQuestionController.showClearDatabaseQuestion,
        requirement = ItemRequirements.IfUserAnswersItemIs(
          CreateNewDatabaseQuestion,
          {
            case TrueOrFalseForm(false) => true
            case _                      => false
          }
        ),
        journeyId = JourneyId.ImportGedcom,
        index = 6,
        checkYourAnswerWrites = Some(messages => TrueOrFalseForm.cyaWrites(using messages))
      )(using Json.format[TrueOrFalseForm])

  case SelectExistingDatabaseQuestion
      extends UserAnswersKey[SelectExistingDatabaseForm](
        page = controllers.gedcom.routes.ImportGedcomController.gedcomDatabaseSelect,
        requirement = ItemRequirements.IfUserAnswersItemIs(
          CreateNewDatabaseQuestion,
          {
            case TrueOrFalseForm(false) => true
            case _                      => false
          }
        ),
        journeyId = JourneyId.ImportGedcom,
        index = 7,
        checkYourAnswerWrites = Some(messages => SelectExistingDatabaseForm.cyaWrites(using messages))
      )(using Json.format[SelectExistingDatabaseForm])

  case NewDatabaseDetailsQuestion
      extends UserAnswersKey[CreateNewDatabaseForm](
        page = controllers.gedcom.routes.ImportGedcomController.addNewDatabase,
        requirement = ItemRequirements.IfUserAnswersItemIs(
          CreateNewDatabaseQuestion,
          {
            case TrueOrFalseForm(true) => true
            case _                     => false
          }
        ),
        journeyId = JourneyId.ImportGedcom,
        index = 8,
      )(using Json.format[CreateNewDatabaseForm])

}
