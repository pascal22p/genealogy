package models

import java.time.Instant

import anorm.~
import anorm.RowParser
import anorm.SqlParser.get
import models.forms.SourRecordForm
import models.SourCitationType.SourCitationType

final case class SourRecord(
    id: Int,
    auth: String,
    title: String,
    abbr: String,
    publ: String,
    agnc: String,
    rin: String,
    repoId: Option[Int],
    repoCaln: String,
    repoMedi: String,
    timestamp: Instant
) {
  def toForm(parentId: Int, parentType: SourCitationType): SourRecordForm =
    SourRecordForm(auth, title, abbr, publ, agnc, rin, repoCaln, repoMedi, parentId, parentType)

  def fromForm(form: SourRecordForm): SourRecord = SourRecord(
    id,
    form.auth,
    form.title,
    form.abbr,
    form.publ,
    form.agnc,
    form.rin,
    repoId,
    form.repoCaln,
    form.repoMedi,
    timestamp
  )
}

object SourRecord {
  val mysqlParser: RowParser[SourRecord] = {
    (get[Int]("sour_records_id") ~
      get[String]("sour_records_auth") ~
      get[String]("sour_records_title") ~
      get[String]("sour_records_abbr") ~
      get[String]("sour_records_publ") ~
      get[String]("sour_records_agnc") ~
      get[String]("sour_records_rin") ~
      get[Option[Int]]("repo_id") ~
      get[String]("repo_caln") ~
      get[String]("repo_medi") ~
      get[Option[Instant]]("sour_records_timestamp")).map {
      case id ~ auth ~ title ~ abbr ~ publ ~ agnc ~ rin ~ repoId ~ repoCaln ~ repoMedi ~ timestamp =>
        SourRecord(id, auth, title, abbr, publ, agnc, rin, repoId, repoCaln, repoMedi, timestamp.getOrElse(Instant.now))
    }
  }
}
