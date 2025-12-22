package models.journeyCache

sealed trait ItemRequirements

object ItemRequirements {
  final case class Always() extends ItemRequirements

  final case class IfUserAnswersItemIs[A <: UserAnswersItem](
      item: UserAnswersKey[A],
      predicate: UserAnswersItem => Boolean
  ) extends ItemRequirements
}
