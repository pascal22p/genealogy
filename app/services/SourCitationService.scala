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
import models.SourRecord
import queries.GetSqlQueries

@Singleton
class SourCitationService @Inject() (mariadbQueries: GetSqlQueries)(
    implicit ec: ExecutionContext
) {
  def getSourCitations(
      sourCitationId: Int,
      sourCitationType: SourCitationType,
      dbId: Int
  ): Future[List[SourCitation]] = {
    mariadbQueries.getSourCitations(sourCitationId, sourCitationType, dbId).flatMap { sources =>
      sources.traverse { source =>
        fillExtraData(source)
      }
    }
  }

  private def fillExtraData(sourCitationQueryData: SourCitationQueryData): Future[SourCitation] = {
    for {
      medias: List[Media] <- mariadbQueries.getMedias(
        Some(sourCitationQueryData.id),
        SourCitationMedia,
        sourCitationQueryData.dbId
      )
    } yield {
      SourCitation(sourCitationQueryData, medias)
    }
  }

  def getSourRecords(dbId: Int): Future[List[SourRecord]] = {
    mariadbQueries.getSourRecords(dbId)
  }

}
