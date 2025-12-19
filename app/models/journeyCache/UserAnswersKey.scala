package models.journeyCache

import models.forms.DatabaseForm
import models.forms.GedcomListForm
import models.forms.TrueOrFalseForm
import play.api.libs.json.*
import play.api.mvc.Call

enum UserAnswersKey[A <: UserAnswersItem](
    val page: Call,
    val requirement: ItemRequirements,
    val checkYourAnswerFormat: Option[OFormat[A]] = None // optional format for check your answers page.
)(using val format: OFormat[A]) { // format used to serialize/deserialize A (not used currently). Also used as default if checkYourAnswerFormat is None.

  /*
  // Not needed currently, but kept for reference. Used to serialize UserAnswers to Json for storage
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  def writeAsJson(value: UserAnswersItem): JsValue =
    format.writes(value.asInstanceOf[A])

  def readFromJson(json: JsValue): JsResult[UserAnswersItem] =
    format.reads(json)

   */

  case GedcomPath
      extends UserAnswersKey[GedcomListForm](
        page = controllers.gedcom.routes.ImportGedcomController.showGedcomList,
        requirement = ItemRequirements.Always()
      )(using Json.format[GedcomListForm])

  case NewDatabaseQuestion
      extends UserAnswersKey[TrueOrFalseForm](
        page = controllers.gedcom.routes.ImportGedcomController.showNewDatabaseQuestion,
        requirement = ItemRequirements.Always()
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

/*
// Not needed currently, but kept for reference. Used to serialize UserAnswers to Json for storage
object UserAnswersKey {
  implicit val userAnswersItemWrites: Writes[UserAnswersKey[?]] = Writes { item =>
    JsString(s"$item")
  }

  implicit val userAnswersItemReads: Reads[UserAnswersKey[?]] = Reads {
    case JsString(name) => JsSuccess(UserAnswersKey.valueOf(item))
    case _ => JsError("String value expected")
  }
}
 */
