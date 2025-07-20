package models

import anorm.~
import anorm.RowParser
import anorm.SqlParser.get

final case class GenealogyDatabase(
    id: Int,
    name: String,
    description: String,
    medias: Option[String]
)

object GenealogyDatabase {
  val mysqlParser: RowParser[GenealogyDatabase] =
    (get[Int]("id") ~
      get[String]("nom") ~
      get[String]("descriptif") ~
      get[Option[String]]("medias")).map {
      case id ~ name ~ description ~ medias =>
        GenealogyDatabase(id, name, description, medias)
    }
}
