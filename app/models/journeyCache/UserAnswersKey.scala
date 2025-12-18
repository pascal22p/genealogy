package models.journeyCache

import models.forms.DatabaseForm
import models.forms.GedcomListForm
import models.forms.TrueOrFalseForm
import models.forms.UserAnswersItem
import play.api.libs.json.*
import play.api.mvc.Call

enum UserAnswersKey[A <: UserAnswersItem](
    val page: Call,
    val requirement: ItemRequirements
)(using val format: OFormat[A]) {

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  def writeAsJson(value: UserAnswersItem): JsValue =
    format.asInstanceOf[OFormat[UserAnswersItem]].writes(value)

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  def readFromJson(json: JsValue): JsResult[UserAnswersItem] =
    format.asInstanceOf[OFormat[UserAnswersItem]].reads(json)

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
        requirement = ItemRequirements.IfCaseClassFormsIs(
          NewDatabaseQuestion,
          {
            case TrueOrFalseForm(true) => true
            case _                     => false
          }
        )
      )(using Json.format[DatabaseForm])

}

object UserAnswersKey {
  implicit val userAnswersItemWrites: Writes[UserAnswersKey[?]] = Writes { item =>
    JsString(s"$item")
  }

  implicit val userAnswersItemReads: Reads[UserAnswersKey[?]] = Reads {
    case JsString(name) =>
      UserAnswersKey.values.find(item => s"$item" == name) match {
        case Some(item) => JsSuccess(item)
        case None       => JsError(s"Unknown UserAnswersItem: $name")
      }
    case _ => JsError("String value expected")
  }
}
