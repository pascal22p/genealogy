package models.journeyCache

import scala.annotation.tailrec

import models.journeyCache.UserAnswersKey
import play.api.i18n.Messages
import play.api.libs.json.*
import play.api.mvc.Call
import cats.implicits.*

final case class UserAnswers(
    data: Map[UserAnswersKey[?], UserAnswersItem]
) {
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  def getItem[A <: UserAnswersItem](key: UserAnswersKey[A]): A = {
    data(key).asInstanceOf[A]
  }

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  def getOptionalItem[A <: UserAnswersItem](key: UserAnswersKey[A]): Option[A] = {
    data.get(key).map(_.asInstanceOf[A])
  }

  def upsert[A <: UserAnswersItem](key: UserAnswersKey[A], value: A): UserAnswers = {
    UserAnswers(data + (key -> value))
  }

  // validate userAnswers data from a journey to check if every element required is present and an element not required is removed.
  // Elements not required but present exists when the user go back to change his answers.
  def validated(journeyId: JourneyId): Either[Call, UserAnswers] = {
    UserAnswersKey.values.toList
      .filter(_.journeyId == journeyId)
      .traverse {
        case key if key.requirement match { // if the item is required but missing from userAnswers
              case ItemRequirements.Always() => !data.contains(key)
              case _                         => false
            } =>
          Left(key.page) // then return the page to redirect to so that the missing item can be filled in

        case key =>
          if (isValid(key)) { Right(Some(key)) } // The required key exist and is valid
          else { Right(None) }                   // The key is optional and can be ignored
      }
      .map(keysList => UserAnswers(data.filter(key => keysList.flatten.contains(key._1))))
  }

  // Extract individual items from a case class recursively into a flat map of key-value pairs
  // So that each value is displayed separately on the Check Your Answers page
  // This behaviour can be overridden for specific keys by providing a custom OWrites via checkYourAnswerWrites
  def flattenByKey(journeyId: JourneyId)(using messages: Messages): Map[UserAnswersKey[?], Map[String, String]] = {
    data.filter(_._1.journeyId == journeyId).map {
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
          data.get(depKey) match {
            case Some(v) =>
              predicate(v) && isValid(depKey, seen + key)
            case None =>
              false
          }
      }
    }

}
