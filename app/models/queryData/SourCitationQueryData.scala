package models

import java.time.Instant

import anorm.*
import anorm.SqlParser.*
import anorm.SqlParser.get
import models.SourCitationType.SourCitationType

final case class SourCitationQueryData(
    id: Int,
    record: Option[SourRecord],
    page: String,
    even: String,
    role: String,
    dates: String,
    text: String,
    quay: Option[Int],
    subm: String,
    timestamp: Instant,
    ownerId: Option[Int],
    sourceType: SourCitationType
)

object SourCitationQueryData {
  val mysqlParser: RowParser[SourCitationQueryData] =
    (get[Int]("genea_sour_citations.sour_citations_id") ~
      get[Option[Int]]("sour_records_id") ~
      get[String]("sour_citations_page") ~
      get[String]("sour_citations_even") ~
      get[String]("sour_citations_even_role") ~
      get[String]("sour_citations_data_dates") ~
      get[String]("sour_citations_data_text") ~
      get[Option[Int]]("sour_citations_quay") ~
      get[String]("sour_citations_subm") ~
      get[Option[Instant]]("sour_citations_timestamp") ~
      get[Option[Int]]("sour_records_id") ~
      get[String]("sour_records_auth") ~
      get[String]("sour_records_title") ~
      get[String]("sour_records_abbr") ~
      get[String]("sour_records_publ") ~
      get[String]("sour_records_agnc") ~
      get[String]("sour_records_rin") ~
      get[Option[Instant]]("sour_records_timestamp") ~
      get[Option[Int]]("genea_sour_records.repo_id") ~
      get[String]("repo_caln") ~
      get[String]("repo_medi") ~
      get[Option[Int]]("owner_id") ~
      get[String]("source_type")).map {
      case id ~ recordId ~ page ~ even ~ role ~ dates ~ text ~ quay ~ subm ~ timeStamp ~
          sourRecordId ~ auth ~ title ~ abbr ~ publ ~ agnc ~ rin ~ sourRecordTimestamp ~ repoId ~ repoCaln ~ repoMedi ~
          ownerId ~ sourceType =>
        SourCitationQueryData(
          id,
          sourRecordId.map(id =>
            SourRecord(
              id,
              auth,
              title,
              abbr,
              publ,
              agnc,
              rin,
              repoId,
              repoCaln,
              repoMedi,
              sourRecordTimestamp.getOrElse(Instant.now)
            )
          ),
          page,
          even,
          role,
          dates,
          text,
          quay,
          subm,
          timeStamp.getOrElse(Instant.now),
          ownerId,
          SourCitationType.fromString(sourceType)
        )
    }
}
