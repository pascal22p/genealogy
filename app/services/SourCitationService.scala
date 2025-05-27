package services

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.data.OptionT
import cats.implicits.*
import models.Media
import models.MediaType.SourCitationMedia
import models.SourCitation
import models.SourCitationQueryData
import models.SourCitationType.SourCitationType
import models.SourCitationType.UnknownSourCitation
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

  def getSourCitation(sourCitationId: Int, dbId: Int): OptionT[Future, SourCitation] = {
    OptionT(
      mariadbQueries
        .getSourCitations(sourCitationId, UnknownSourCitation, dbId)
        .flatMap { sources =>
          sources.traverse { source =>
            fillExtraData(source)
          }
        }
        .map {
          case Nil                 => None
          case sourCitation :: Nil => Some(sourCitation)
          case sourCitations =>
            throw new IllegalStateException(
              s"Expected one sour citation, but found multiple: ${sourCitations.size}"
            )
        }
    )
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
