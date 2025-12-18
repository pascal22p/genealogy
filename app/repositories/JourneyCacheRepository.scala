package repositories

import javax.inject.Inject
import javax.inject.Singleton

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import models.forms.UserAnswersItem
import models.journeyCache.UserAnswers
import models.journeyCache.UserAnswersKey
import models.AuthenticatedRequest

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
class InMemoryJourneyCacheRepository @Inject() () extends JourneyCacheRepository {

  private val store: TrieMap[String, Map[UserAnswersKey[?], UserAnswersItem]] =
    TrieMap.empty

  private def journeyId(implicit request: AuthenticatedRequest[?]): String =
    request.localSession.sessionId

  override def get(implicit ec: ExecutionContext, request: AuthenticatedRequest[?]): Future[Option[UserAnswers]] =
    Future.successful {
      store.get(journeyId).map { data =>
        UserAnswers(journeyId, data)
      }
    }

  def get[A <: UserAnswersItem](
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
  )(implicit ec: ExecutionContext, request: AuthenticatedRequest[?]): Future[UserAnswers] =
    Future.successful {
      val updatedData =
        store.getOrElse(journeyId, Map.empty) + (key -> value)

      store.put(journeyId, updatedData)

      UserAnswers(journeyId, updatedData)
    }

  override def clear(implicit ec: ExecutionContext, request: AuthenticatedRequest[?]): Future[Unit] =
    Future.successful {
      store.remove(journeyId)
      ()
    }
}
