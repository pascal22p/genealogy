package models.journeyCache

object JourneyValidation {

  extension (answers: Map[UserAnswersKey[?], UserAnswersItem]) {
    def validate: List[UserAnswersKey[?]] =
      answers.keys.flatMap { keyToCheck =>
        keyToCheck.requirement match {
          case ItemRequirements.IfCaseClassFormsIs(key, predicate) =>
            answers.get(key) match {
              case Some(v) if predicate(v) => Nil
              case _                       => List(keyToCheck)
            }

          case _ => Nil
        }
      }.toList
  }

}
