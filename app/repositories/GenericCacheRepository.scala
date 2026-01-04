package repositories

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.implicits.*
import models.AuthenticatedRequest
import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import queries.JourneyCacheQueries

// unused and incomplete. journeyCacheQueries needs to be refactored to be more generic.
// allows to cache any resource not only user answers.
abstract class GenericCacheRepository[A](protected val journeyCacheQueries: JourneyCacheQueries)(
    implicit protected val ec: ExecutionContext
) {

  def cache(
      key: String
  )(f: => Future[A])(implicit request: AuthenticatedRequest[?], format: Format[A]): Future[A] = {
    def fetchAndCache(implicit format: Format[A]): Future[A] = for {
      result <- f
      _      <- journeyCacheQueries.upsertUserAnswers(sessionId, Json.toJson(result).toString, key)
    } yield result

    journeyCacheQueries.getUserAnswers(sessionId, key).flatMap {
      case Some(value) =>
        journeyCacheQueries.updateLastUpdated(sessionId, key).map { _ =>
          Json.parse(value._2).as[A]
        }
      case None => fetchAndCache
    }
  }

  def sessionId(implicit request: AuthenticatedRequest[?]): String = request.localSession.sessionId

  def get(key: String)(implicit request: AuthenticatedRequest[?], reads: Reads[A]): Future[Option[A]] =
    journeyCacheQueries.getUserAnswers(sessionId, key).flatMap { maybeResult =>
      maybeResult.traverse { result =>
        journeyCacheQueries.updateLastUpdated(sessionId, key).map { _ =>
          Json.parse(result._2).as[A]
        }
      }
    }

  def put(key: String, value: A)(implicit request: AuthenticatedRequest[?], writes: Writes[A]): Future[Unit] =
    journeyCacheQueries.upsertUserAnswers(sessionId, Json.toJson(value).toString, key).map(_ => ())
}
