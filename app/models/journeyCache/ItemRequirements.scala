package models.journeyCache

import models.forms.UserAnswersItem

sealed trait ItemRequirements

object ItemRequirements {
  final case class Always() extends ItemRequirements

  final case class IfCaseClassFormsIs[A <: UserAnswersItem](
      item: UserAnswersKey[A],
      predicate: UserAnswersItem => Boolean
  ) extends ItemRequirements
}
