package models.journeyCache

import scala.annotation.tailrec

object JourneyValidation {

  extension (answers: Map[UserAnswersKey[?], UserAnswersItem]) {
    def validateRecursive: List[UserAnswersKey[?]] =
      answers.keys.filterNot { key =>
        isValid(key)
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

  }
}
