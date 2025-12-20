package models.journeyCache

import models.journeyCache.JourneyValidation.validateRecursive
import models.journeyCache.UserAnswersKey
import play.api.i18n.Messages
import play.api.libs.json.*

final case class UserAnswers(
    sessionId: String,
    data: Map[UserAnswersKey[?], UserAnswersItem]
) {
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  def get[A <: UserAnswersItem](key: UserAnswersKey[A]): Option[A] =
    data.get(key).map(_.asInstanceOf[A])

  def validated: UserAnswers = UserAnswers(sessionId, data.filterNot((key, _) => data.validateRecursive.contains(key)))

  final case class UserAnswers(
      journeyId: String,
      data: Map[UserAnswersKey[?], UserAnswersItem]
  ) {

    def flattenByKey(using messages: Messages): Map[UserAnswersKey[?], Map[String, String]] = {
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
    )(using messages: Messages): Map[String, String] = {
      val jsonValueWrites = key.checkYourAnswerWrites.fold(key.format)(_.apply(messages))
      val jsValue         = Json.toJson(item.asInstanceOf[A])(using jsonValueWrites)

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
