package models.queryData

import java.time.Instant

import anorm.*
import anorm.SqlParser.*

final case class EventDetailOnlyQueryData(
    base: Int,
    events_details_id: Int,
    place_id: Option[Int],
    addr_id: Option[Int],
    events_details_descriptor: String,
    events_details_gedcom_date: String,
    events_details_age: String,
    events_details_cause: String,
    jd_count: Option[Int],
    jd_precision: Option[Int],
    jd_calendar: Option[String],
    events_details_famc: Option[Int],
    events_details_adop: Option[String],
    events_details_timestamp: Instant
)

object EventDetailOnlyQueryData {
  val mysqlParser: RowParser[EventDetailOnlyQueryData] =
    (get[Int]("base") ~
      get[Int]("genea_events_details.events_details_id") ~
      get[Option[Int]]("place_id") ~
      get[Option[Int]]("addr_id") ~
      get[String]("events_details_descriptor") ~
      get[String]("events_details_gedcom_date") ~
      get[String]("events_details_age") ~
      get[String]("events_details_cause") ~
      get[Option[Int]]("jd_count") ~
      get[Option[Int]]("jd_precision") ~
      get[Option[String]]("jd_calendar") ~
      get[Option[Int]]("events_details_famc") ~
      get[Option[String]]("events_details_adop") ~
      get[Option[Instant]]("events_details_timestamp")).map {
      case base ~ events_details_id ~ place_id ~ addr_id ~
          events_details_descriptor ~ events_details_gedcom_date ~ events_details_age ~ events_details_cause ~
          jd_count ~ jd_precision ~ jd_calendar ~
          events_details_famc ~ events_details_adop ~ events_details_timestamp =>
        EventDetailOnlyQueryData(
          base,
          events_details_id,
          place_id,
          addr_id,
          events_details_descriptor,
          events_details_gedcom_date,
          events_details_age,
          events_details_cause,
          jd_count,
          jd_precision,
          jd_calendar,
          events_details_famc,
          events_details_adop,
          events_details_timestamp.getOrElse(Instant.now)
        )
    }
}
