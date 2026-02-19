package models.queryData

import anorm.*
import anorm.SqlParser.*

final case class RepositoryQueryData(id: Int, base: Int, name: String, rin: String, address: Option[AddressQueryData])

object RepositoryQueryData {
  val mysqlParserRepository: RowParser[RepositoryQueryData] =
    (get[Int]("repo_id") ~
      get[Int]("base") ~
      get[String]("repo_name") ~
      get[String]("repo_rin") ~
      get[Option[Int]]("addr_id") ~
      get[String]("addr_addr") ~
      get[String]("addr_city") ~
      get[String]("addr_stae") ~
      get[String]("addr_post") ~
      get[String]("addr_ctry") ~
      get[String]("addr_phon1") ~
      get[String]("addr_email1") ~
      get[String]("addr_fax1") ~
      get[String]("addr_www1")).map {
      case id ~ base ~ name ~ rin ~ addr_id ~ addr_addr ~ addr_city ~ addr_stae ~ addr_post ~ addr_ctry ~ addr_phon1 ~ addr_email1 ~ addr_fax1 ~ addr_www1 =>
        val address = addr_id.flatMap { id =>
          if (
            Seq(addr_addr, addr_city, addr_stae, addr_post, addr_ctry, addr_phon1, addr_email1, addr_fax1, addr_www1)
              .forall(_.isEmpty)
          ) {
            None
          } else {
            Some(
              AddressQueryData(
                id,
                addr_addr,
                addr_city,
                addr_stae,
                addr_post,
                addr_ctry,
                addr_phon1,
                "",
                "",
                addr_email1,
                "",
                "",
                addr_fax1,
                "",
                "",
                addr_www1,
                "",
                ""
              )
            )
          }
        }

        RepositoryQueryData(
          id = id,
          base = base,
          name = name,
          rin = rin,
          address = address
        )
    }
}
