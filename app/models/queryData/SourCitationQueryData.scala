package models

import java.time.Instant

import anorm.*
import anorm.SqlParser.*
import models.SourCitationType.SourCitationType
import models.SourCitationType.UnknownSourCitation

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
    sourceType: SourCitationType,
    dbId: Int
)

object SourCitationQueryData {
  val mysqlParser: RowParser[SourCitationQueryData] =
    (get[Int]("genea_sour_citations.sour_citations_id") ~
      get[String]("sour_citations_page") ~
      get[String]("sour_citations_even") ~
      get[String]("sour_citations_even_role") ~
      get[String]("sour_citations_data_dates") ~
      get[String]("sour_citations_data_text") ~
      get[Option[Int]]("sour_citations_quay") ~
      get[String]("sour_citations_subm") ~
      get[Option[Instant]]("sour_citations_timestamp") ~
      get[Option[Int]]("sour_records_id") ~
      get[Option[String]]("sour_records_auth") ~
      get[Option[String]]("sour_records_title") ~
      get[Option[String]]("sour_records_abbr") ~
      get[Option[String]]("sour_records_publ") ~
      get[Option[String]]("sour_records_agnc") ~
      get[Option[String]]("sour_records_rin") ~
      get[Option[Instant]]("sour_records_timestamp") ~
      get[Option[Int]]("genea_sour_records.repo_id") ~
      get[Option[String]]("repo_caln") ~
      get[Option[String]]("repo_medi") ~
      get[Option[Int]]("owner_id") ~
      get[String]("source_type") ~
      get[Int]("genea_sour_citations.base")).map {
      case id ~ page ~ even ~ role ~ dates ~ text ~ quay ~ subm ~ timeStamp ~
          sourRecordId ~ auth ~ title ~ abbr ~ publ ~ agnc ~ rin ~ sourRecordTimestamp ~ repoId ~ repoCaln ~ repoMedi ~
          ownerId ~ sourceType ~ base =>
        SourCitationQueryData(
          id,
          sourRecordId.map(id =>
            SourRecord(
              id,
              auth.getOrElse(""),
              title.getOrElse(""),
              abbr.getOrElse(""),
              publ.getOrElse(""),
              agnc.getOrElse(""),
              rin.getOrElse(""),
              repoId,
              repoCaln.getOrElse(""),
              repoMedi.getOrElse(""),
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
          SourCitationType.fromString(sourceType),
          base
        )
    }

  val mysqlParserCitationOnly: RowParser[SourCitationQueryData] =
    (get[Int]("genea_sour_citations.sour_citations_id") ~
      get[String]("sour_citations_page") ~
      get[String]("sour_citations_even") ~
      get[String]("sour_citations_even_role") ~
      get[String]("sour_citations_data_dates") ~
      get[String]("sour_citations_data_text") ~
      get[Option[Int]]("sour_citations_quay") ~
      get[String]("sour_citations_subm") ~
      get[Option[Instant]]("sour_citations_timestamp") ~
      get[Int]("genea_sour_citations.base")).map {
      case id ~ page ~ even ~ role ~ dates ~ text ~ quay ~ subm ~ timeStamp ~ base =>
        SourCitationQueryData(
          id,
          None,
          page,
          even,
          role,
          dates,
          text,
          quay,
          subm,
          timeStamp.getOrElse(Instant.now),
          None,
          UnknownSourCitation,
          base
        )
    }
}
