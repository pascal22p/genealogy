package models.journeyCache

import models.forms.CaseClassForms

object JourneyValidation {

  extension (answers: Map[UserAnswersItem, CaseClassForms]) {
    def validate: List[UserAnswersItem] =
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
