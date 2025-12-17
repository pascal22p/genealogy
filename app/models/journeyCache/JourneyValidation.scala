package models.journeyCache

object JourneyValidation {

  extension (answers: Map[JourneyCacheItem, String]) {
    def validate: List[JourneyCacheItem] =
      answers.keys.flatMap { keyToCheck =>
        keyToCheck.requirement match {
          case ItemRequirements.Always() => Nil

          case ItemRequirements.IfStringIs(key, expected) =>
            answers.get(key) match {
              case Some(v) if v == expected => Nil
              case _                        => List(keyToCheck)
            }
        }
      }.toList
  }

}
