package models.journeyCache

import models.forms.CaseClassForms

sealed trait ItemRequirements

object ItemRequirements {
  final case class Always() extends ItemRequirements

  final case class IfCaseClassFormsIs(
      item: UserAnswersItem,
      predicate: CaseClassForms => Boolean
  ) extends ItemRequirements
}
