package models.queryData

import java.time.Instant

import anorm.*
import anorm.SqlParser.*
import models.EventType
import models.EventType.EventType
import models.EventType.UnknownEvent
import models.ResnType

final case class EventDetailQueryData(
    dbId: Int,
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
    events_details_timestamp: Instant,
    tag: Option[String],
    description: Option[String],
    eventType: EventType,
    sourCount: Int,
    ownerId: Option[Int],
    resn: Option[ResnType.ResnType]
)

object EventDetailQueryData {
  val mysqlParser: RowParser[EventDetailQueryData] =
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
      get[Option[Instant]]("events_details_timestamp") ~
      get[Option[String]]("tag") ~
      get[Option[String]]("description") ~
      get[String]("event_type") ~
      get[Option[Int]]("sourCount") ~
      get[Option[Int]]("ownerId") ~
      get[Option[String]]("resn")).map {
      case base ~ events_details_id ~ place_id ~ addr_id ~
          events_details_descriptor ~ events_details_gedcom_date ~ events_details_age ~ events_details_cause ~
          jd_count ~ jd_precision ~ jd_calendar ~
          events_details_famc ~ events_details_adop ~ events_details_timestamp ~ tag ~ description ~ eventType ~
          sourCount ~ ownerId ~ resn =>
        EventDetailQueryData(
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
          events_details_timestamp.getOrElse(Instant.now),
          tag,
          description,
          EventType.fromString(eventType),
          sourCount.getOrElse(0),
          ownerId,
          resn.flatMap(ResnType.fromString)
        )
    }

  val mysqlParserEventDetailOnly: RowParser[EventDetailQueryData] =
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
        EventDetailQueryData(
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
          events_details_timestamp.getOrElse(Instant.now),
          None,
          None,
          UnknownEvent,
          0,
          None,
          None
        )
    }
}
