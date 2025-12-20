package models.journeyCache

import models.forms.DatabaseForm
import models.forms.GedcomListForm
import models.forms.TrueOrFalseForm
import play.api.i18n.Messages
import play.api.libs.json.*
import play.api.mvc.Call

enum UserAnswersKey[A <: UserAnswersItem](
    val page: Call,
    val requirement: ItemRequirements,
    val checkYourAnswerWrites: Option[Messages => OWrites[A]] = None // optional format for check your answers page.
)(using val format: OFormat[A]) { // format used to serialize/deserialize A. Also used as default if checkYourAnswerFormat is None.

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  def writeUserAnswersItemAsJson(value: UserAnswersItem): JsValue =
    format.writes(value.asInstanceOf[A])

  def readUserAnswersItemFromJson(json: JsValue): JsResult[UserAnswersItem] =
    format.reads(json)

  case GedcomPath
      extends UserAnswersKey[GedcomListForm](
        page = controllers.gedcom.routes.ImportGedcomController.showGedcomList,
        requirement = ItemRequirements.Always()
      )(using Json.format[GedcomListForm])

  case NewDatabaseQuestion
      extends UserAnswersKey[TrueOrFalseForm](
        page = controllers.gedcom.routes.ImportGedcomController.showNewDatabaseQuestion,
        requirement = ItemRequirements.Always(),
        checkYourAnswerWrites = Some(messages => TrueOrFalseForm.cyaWrites(using messages))
      )(using Json.format[TrueOrFalseForm])

  case NewDatabase
      extends UserAnswersKey[DatabaseForm](
        page = controllers.gedcom.routes.ImportGedcomController.addNewDatabase,
        requirement = ItemRequirements.IfUserAnswersItemIs(
          NewDatabaseQuestion,
          {
            case TrueOrFalseForm(true) => true
            case _                     => false
          }
        )
      )(using Json.format[DatabaseForm])

}
