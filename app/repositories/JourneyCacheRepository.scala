package repositories

import javax.inject.Inject
import javax.inject.Singleton

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import models.journeyCache.JourneyCache
import models.journeyCache.JourneyCacheItem

trait JourneyCacheRepository {

  def get(journeyId: String)(implicit ec: ExecutionContext): Future[Option[JourneyCache]]

  def upsert(
      journeyId: String,
      key: JourneyCacheItem,
      value: String
  )(implicit ec: ExecutionContext): Future[JourneyCache]

  def clear(journeyId: String)(implicit ec: ExecutionContext): Future[Unit]
}

@Singleton
class InMemoryJourneyCacheRepository @Inject() () extends JourneyCacheRepository {

  private val store: TrieMap[String, Map[JourneyCacheItem, String]] =
    TrieMap.empty

  override def get(
      journeyId: String
  )(implicit ec: ExecutionContext): Future[Option[JourneyCache]] =
    Future.successful {
      store.get(journeyId).map { data =>
        JourneyCache(journeyId, data)
      }
    }

  override def upsert(
      journeyId: String,
      key: JourneyCacheItem,
      value: String
  )(implicit ec: ExecutionContext): Future[JourneyCache] =
    Future.successful {
      val updatedData =
        store.getOrElse(journeyId, Map.empty) + (key -> value)

      store.put(journeyId, updatedData)

      JourneyCache(journeyId, updatedData)
    }

  override def clear(
      journeyId: String
  )(implicit ec: ExecutionContext): Future[Unit] =
    Future.successful {
      store.remove(journeyId)
      ()
    }
}
