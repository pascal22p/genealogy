package models.journeyCache

import scala.annotation.tailrec

object JourneyValidation {

  extension (answers: Map[UserAnswersKey[?], UserAnswersItem]) {
    def validate: List[UserAnswersKey[?]] =
      answers.keys.flatMap { keyToCheck =>
        keyToCheck.requirement match {
          case ItemRequirements.IfUserAnswersItemIs(key, predicate) =>
            answers.get(key) match {
              case Some(v) if predicate(v) => Nil
              case _                       => List(keyToCheck)
            }

          case _ => Nil
        }
      }.toList

    @tailrec
    private def isValid(
        key: UserAnswersKey[?],
        seen: Set[UserAnswersKey[?]] = Set.empty
    ): Boolean =
      if (seen.contains(key)) {
        throw new RuntimeException(
          s"Cyclic dependency detected: ${(seen + key).mkString(" -> ")}"
        )
      } else {
        key.requirement match {
          case ItemRequirements.Always() =>
            true

          case ItemRequirements.IfUserAnswersItemIs(depKey, predicate) =>
            answers.get(depKey) match {
              case Some(v) =>
                predicate(v) && isValid(depKey, seen + key)
              case None =>
                false
            }
        }
      }

    def validateRecursive: List[UserAnswersKey[?]] =
      answers.keys.filterNot { key =>
        isValid(key)
      }.toList

  }
}
