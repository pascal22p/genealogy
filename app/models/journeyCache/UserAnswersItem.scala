package models.journeyCache

import models.forms.CaseClassForms
import models.forms.DatabaseForm
import models.forms.GedcomListForm
import models.forms.TrueOrFalseForm
import play.api.mvc.Call

sealed trait UserAnswersItem extends Product {
  type Value <: CaseClassForms
  def name: String                  = productPrefix
  def requirement: ItemRequirements = ItemRequirements.Always()
  def page: Call
}

case object GedcomPath extends UserAnswersItem {
  type Value = GedcomListForm
  override def page: Call = controllers.gedcom.routes.ImportGedcomController.showGedcomList
}
case object NewDatabaseQuestion extends UserAnswersItem {
  type Value = TrueOrFalseForm
  override def page: Call = controllers.gedcom.routes.ImportGedcomController.showNewDatabaseQuestion
}
case object NewDatabase extends UserAnswersItem {
  type Value = DatabaseForm
  val predicate: CaseClassForms => Boolean = {
    case TrueOrFalseForm(true) => true
    case _                     => false
  }
  override def requirement: ItemRequirements =
    ItemRequirements.IfCaseClassFormsIs(NewDatabaseQuestion, predicate)
  override def page: Call = controllers.gedcom.routes.ImportGedcomController.addNewDatabase
}
