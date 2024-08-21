package services

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.implicits.*
import models.Media
import models.MediaType.SourCitationMedia
import models.SourCitation
import models.SourCitationQueryData
import models.SourCitationType.SourCitationType
import queries.MariadbQueries

@Singleton
class SourCitationService @Inject() (mariadbQueries: MariadbQueries)(
    implicit ec: ExecutionContext
) {
  def getSourCitations(sourCitationId: Int, sourCitationType: SourCitationType): Future[List[SourCitation]] = {
    mariadbQueries.getSourCitations(sourCitationId, sourCitationType).flatMap { sources =>
      sources.traverse { source =>
        fillExtraData(source)
      }
    }
  }

  private def fillExtraData(sourCitationQueryData: SourCitationQueryData): Future[SourCitation] = {
    for {
      medias: List[Media] <- mariadbQueries.getMedias(sourCitationQueryData.id, SourCitationMedia)
    } yield {
      SourCitation(sourCitationQueryData, medias)
    }
  }

}
