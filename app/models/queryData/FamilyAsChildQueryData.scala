package models.queryData

import java.time.Instant

import anorm._
import anorm.SqlParser._

final case class FamilyAsChildQueryData(
    family: FamilyQueryData,
    refnType: String,
    relaType: String,
    relaStat: Option[String]
)

object FamilyAsChildQueryData {
  val mysqlParser: RowParser[FamilyAsChildQueryData] =
    (get[Int]("familles_id") ~
      get[Option[Int]]("familles_wife") ~
      get[Option[Int]]("familles_husb") ~
      get[Option[Instant]]("familles_timestamp") ~
      get[String]("familles_refn") ~
      get[String]("familles_refn_type") ~
      get[String]("rela_type") ~
      get[Option[String]]("rela_stat")).map {
      case id ~ wife ~ husb ~ timestamp ~ refn ~ refnType ~ relaType ~ relaStat =>
        FamilyAsChildQueryData(
          FamilyQueryData(id, husb, wife, timestamp.getOrElse(Instant.now), None, refn, refnType),
          refnType,
          relaType,
          relaStat
        )
    }
}
