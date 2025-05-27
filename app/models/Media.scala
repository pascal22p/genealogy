package models

import java.time.Instant

import anorm.~
import anorm.RowParser
import anorm.SqlParser.get
import models.MediaType.MediaType
import models.MediaType.UnknownMedia

final case class Media(
    id: Int,
    dbId: Int,
    title: String,
    format: String,
    filename: String,
    timestamp: Instant,
    ownerId: Option[Int],
    mediaType: MediaType
)

object Media {
  val mysqlParser: RowParser[Media] =
    (get[Int]("genea_multimedia.media_id") ~
      get[String]("media_title") ~
      get[String]("media_format") ~
      get[String]("media_file") ~
      get[Option[Instant]]("media_timestamp") ~
      get[Option[Int]]("owner_id") ~
      get[String]("media_type") ~
      get[Int]("base")).map {
      case id ~ title ~ format ~ filename ~ timestamp ~ ownerId ~ mediaType ~ base =>
        Media(
          id,
          base,
          title,
          format,
          filename,
          timestamp.getOrElse(Instant.now()),
          ownerId,
          MediaType.fromString(mediaType)
        )
    }

  val mysqlParserMediaOnly: RowParser[Media] =
    (get[Int]("media_id") ~
      get[String]("media_title") ~
      get[String]("media_format") ~
      get[String]("media_file") ~
      get[Option[Instant]]("media_timestamp") ~
      get[Int]("base")).map {
      case id ~ title ~ format ~ filename ~ timestamp ~ base =>
        Media(
          id,
          base,
          title,
          format,
          filename,
          timestamp.getOrElse(Instant.now()),
          None,
          UnknownMedia
        )
    }
}
