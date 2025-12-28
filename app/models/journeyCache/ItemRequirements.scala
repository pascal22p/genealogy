package models.journeyCache

// Represents the requirements for an item in a user journey.
// Said item can either be always required or conditionally required based on another item's value.
sealed trait ItemRequirements

object ItemRequirements {
  final case class Always() extends ItemRequirements

  final case class IfUserAnswersItemIs[A <: UserAnswersItem](
      item: UserAnswersKey[A],
      predicate: UserAnswersItem => Boolean
  ) extends ItemRequirements
}
