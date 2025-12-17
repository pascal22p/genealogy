package models.journeyCache

import models.journeyCache.JourneyCacheItem

final case class JourneyCache(
    journeyId: String,
    data: Map[JourneyCacheItem, String]
)
