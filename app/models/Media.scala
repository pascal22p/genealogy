package models

import java.time.Instant

import anorm.~
import anorm.RowParser
import anorm.SqlParser.get
import models.MediaType.MediaType

final case class Media(
    id: Int,
    title: String,
    format: String,
    filename: String,
    timestamp: Instant,
    ownerId: Option[Int],
    sourceType: MediaType
)

object Media {
  val mysqlParser: RowParser[Media] =
    (get[Int]("genea_multimedia.media_id") ~
      get[String]("media_title") ~
      get[String]("media_format") ~
      get[String]("media_file") ~
      get[Option[Instant]]("media_timestamp") ~
      get[Option[Int]]("owner_id") ~
      get[String]("media_type")).map {
      case id ~ title ~ format ~ filename ~ timestamp ~ ownerId ~ mediaType =>
        Media(id, title, format, filename, timestamp.getOrElse(Instant.now()), ownerId, MediaType.fromString(mediaType))
    }
}
