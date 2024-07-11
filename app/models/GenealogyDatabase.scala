package models

import anorm.~
import anorm.RowParser
import anorm.SqlParser.get

case class GenealogyDatabase(id: Int, name: String, description: String)

object GenealogyDatabase {
  val mysqlParser: RowParser[GenealogyDatabase] =
    (get[Int]("id") ~
      get[String]("nom") ~
      get[String]("descriptif")).map {
      case id ~ name ~ description =>
        GenealogyDatabase(id, name, description)
    }
}
