package models.journeyCache

sealed trait ItemRequirements

object ItemRequirements {
  final case class Always() extends ItemRequirements

  final case class IfStringIs(
      item: JourneyCacheItem,
      value: String
  ) extends ItemRequirements
}
