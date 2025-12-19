package models.journeyCache

import models.journeyCache.JourneyValidation.validateRecursive
import models.journeyCache.UserAnswersKey
import play.api.libs.json.JsArray
import play.api.libs.json.JsBoolean
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.OFormat
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json.Reads

final case class UserAnswers(
    journeyId: String,
    data: Map[UserAnswersKey[?], UserAnswersItem]
) {
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  def get[A <: UserAnswersItem](key: UserAnswersKey[A]): Option[A] =
    data.get(key).map(_.asInstanceOf[A])

  def validated: UserAnswers = UserAnswers(journeyId, data.filterNot((key, _) => data.validateRecursive.contains(key)))

  final case class UserAnswers(
      journeyId: String,
      data: Map[UserAnswersKey[?], UserAnswersItem]
  ) {

    def flattenByKey: Map[UserAnswersKey[?], Map[String, String]] = {
      data.map {
        case (key, value) =>
          val flattened = flattenItem(key, value)
          key -> flattened
      }
    }

    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    private def flattenItem[A <: UserAnswersItem](
        key: UserAnswersKey[A],
        item: UserAnswersItem
    ): Map[String, String] = {
      val jsonValueFormat = key.checkYourAnswerFormat.getOrElse(key.format)
      val jsValue         = Json.toJson(item.asInstanceOf[A])(using jsonValueFormat)

      def flattenJsObject(jsObj: JsObject, prefix: String = ""): Map[String, String] = {
        jsObj.fields.flatMap {
          case (k, JsString(v))   => Map(prefix + k -> v)
          case (k, JsNumber(v))   => Map(prefix + k -> v.toString)
          case (k, JsBoolean(v))  => Map(prefix + k -> v.toString)
          case (k, JsObject(obj)) =>
            flattenJsObject(JsObject(obj), s"$prefix$k.")
          case (k, JsArray(values)) =>
            values.zipWithIndex.flatMap {
              case (v, i) =>
                v match {
                  case o: JsObject => flattenJsObject(o, s"$prefix$k[$i].")
                  case primitive   => Map(s"$prefix$k[$i]" -> Json.stringify(primitive))
                }
            }
          case (k, other) =>
            Map(prefix + k -> Json.stringify(other))
        }.toMap
      }

      flattenJsObject(jsValue.as[JsObject])
    }
  }

}

/*
// Not needed currently, but kept for reference. Used to serialize UserAnswers to Json for storage
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
 */
