package models.journeyCache

import models.forms.CaseClassForms
import models.journeyCache.JourneyValidation.validate
import models.journeyCache.UserAnswersItem

final case class UserAnswers(
    journeyId: String,
    data: Map[UserAnswersItem, CaseClassForms]
) {
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  def get(key: UserAnswersItem): Option[key.Value] = {
    data.get(key).map(_.asInstanceOf[key.Value])
  }

  def validated: UserAnswers = UserAnswers(journeyId, data.filterNot((key, _) => data.validate.contains(key)))
}
