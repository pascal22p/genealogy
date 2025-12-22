package repositories

import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import models.journeyCache.UserAnswers
import models.journeyCache.UserAnswersItem
import models.journeyCache.UserAnswersKey
import models.AuthenticatedRequest
import play.api.libs.json.*
import play.api.Logging
import queries.JourneyCacheQueries
import cats.implicits.*
import utils.CatsApplicatives

trait JourneyCacheRepository {

  def get(implicit ec: ExecutionContext, request: AuthenticatedRequest[?]): Future[Option[UserAnswers]]

  def get[A <: UserAnswersItem](
      key: UserAnswersKey[A]
  )(implicit ec: ExecutionContext, request: AuthenticatedRequest[?]): Future[Option[A]]

  def upsert(
      key: UserAnswersKey[?],
      value: UserAnswersItem
  )(implicit ec: ExecutionContext, request: AuthenticatedRequest[?]): Future[UserAnswers]

  def clear(implicit ec: ExecutionContext, request: AuthenticatedRequest[?]): Future[Unit]
}

@Singleton
class MariadbJourneyCacheRepository @Inject() (journeyCacheQueries: JourneyCacheQueries)
    extends JourneyCacheRepository
    with Logging {

  @SuppressWarnings(Array("org.wartremover.warts.EnumValueOf"))
  private val userAnswersMapFormat: Format[Map[UserAnswersKey[?], UserAnswersItem]] =
    Format(
      Reads { json =>
        json
          .validate[Map[String, JsValue]]
          .flatMap { stringMap =>
            stringMap.toList.traverse { (userAnswersKeyString, userAnswersItemString) =>
              val userAnswersKey = UserAnswersKey.valueOf(userAnswersKeyString)
              userAnswersKey.readUserAnswersItemFromJson(userAnswersItemString).map(userAnswersKey -> _)
            }(using CatsApplicatives.jsResultApplicativeAndApplicativeError)
          }
          .map(_.toMap)
      },
      Writes { map =>
        JsObject(map.map {
          case (k, v) =>
            s"$k" -> k.writeUserAnswersItemAsJson(v)
        })
      }
    )

  private def sessionId(implicit request: AuthenticatedRequest[?]): String =
    request.localSession.sessionId

  override def get(implicit ec: ExecutionContext, request: AuthenticatedRequest[?]): Future[Option[UserAnswers]] =
    journeyCacheQueries.getUserAnswers(sessionId).flatMap {
      case None                                                                                         => Future.successful(None)
      case Some((_, _, lastUpdated)) if lastUpdated.isBefore(Instant.now.minus(Duration.ofMinutes(30))) =>
        Future.successful(None)
      case Some((_, data, _)) =>
        journeyCacheQueries.updateLastUpdated(sessionId).flatMap { _ =>
          Try(Json.parse(data).as[Map[UserAnswersKey[?], UserAnswersItem]](using userAnswersMapFormat)) match {
            case Success(obj) => Future.successful(Some(UserAnswers(sessionId, obj)))
            case Failure(ex)  =>
              journeyCacheQueries.deleteUserAnswers(sessionId).map { _ =>
                throw ex
              }
          }
        }
    }

  override def get[A <: UserAnswersItem](
      key: UserAnswersKey[A]
  )(implicit ec: ExecutionContext, request: AuthenticatedRequest[?]): Future[Option[A]] = {
    get.map {
      case Some(userAnswers) => userAnswers.get(key)
      case _                 => None
    }
  }

  override def upsert(
      key: UserAnswersKey[?],
      value: UserAnswersItem
  )(implicit ec: ExecutionContext, request: AuthenticatedRequest[?]): Future[UserAnswers] = {
    get.flatMap { maybeUserAnswers =>
      val updatedData = maybeUserAnswers.fold(Map.empty[UserAnswersKey[?], UserAnswersItem])(_.data + (key -> value))
      journeyCacheQueries
        .upsertUserAnswers(sessionId, Json.toJson(updatedData)(using userAnswersMapFormat).toString)
        .map(_ => UserAnswers(sessionId, updatedData))
    }
  }

  override def clear(implicit ec: ExecutionContext, request: AuthenticatedRequest[?]): Future[Unit] =
    journeyCacheQueries.deleteUserAnswers(sessionId).map(_ => ())
}
