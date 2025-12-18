package models.journeyCache

import models.journeyCache.JourneyValidation.validate
import models.journeyCache.UserAnswersKey
import play.api.libs.json.Format
import play.api.libs.json.JsError
import play.api.libs.json.JsObject
import play.api.libs.json.JsResult
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.OFormat
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json.Reads
import play.api.libs.json.Writes

final case class UserAnswers(
    journeyId: String,
    data: Map[UserAnswersKey[?], UserAnswersItem]
) {
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  def get[A <: UserAnswersItem](key: UserAnswersKey[A]): Option[A] =
    data.get(key).map(_.asInstanceOf[A])

  def validated: UserAnswers = UserAnswers(journeyId, data.filterNot((key, _) => data.validate.contains(key)))
}

object UserAnswers {
  implicit val userAnswersMapFormat: Format[Map[UserAnswersKey[?], UserAnswersItem]] =
    Format(
      Reads { json =>
        json.validate[Map[String, JsValue]].flatMap { stringMap =>
          val results = stringMap.toList.map {
            case (k, v) =>
              UserAnswersKey.values.find(item => s"$item" == k) match {
                case Some(item) => item.readFromJson(v).map(a => item -> a)
                case None       => JsError(s"Unknown UserAnswersItem: $k")
              }
          }

          results.foldLeft(
            JsSuccess(Map.empty[UserAnswersKey[?], UserAnswersItem]): JsResult[Map[UserAnswersKey[?], UserAnswersItem]]
          ) {
            case (acc, jsRes) =>
              for {
                mapAcc <- acc
                elem   <- jsRes
              } yield mapAcc + elem
          }
        }
      },
      Writes { map =>
        JsObject(map.map {
          case (k, v) =>
            s"$k" -> k.writeAsJson(v)
        })
      }
    )

  implicit val userAnswersFormat: OFormat[UserAnswers] =
    Json.format[UserAnswers]
}
