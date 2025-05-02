package models.queryData

import java.time.Instant

import anorm._
import anorm.SqlParser._

final case class FamilyQueryData(
    id: Int,
    parent1: Option[Int],
    parent2: Option[Int],
    timestamp: Instant,
    privacyRestriction: Option[String],
    refn: String,
    refnType: String,
    base: Int
)

object FamilyQueryData {
  val mysqlParser: RowParser[FamilyQueryData] =
    (get[Int]("familles_id") ~
      get[Option[Int]]("familles_wife") ~
      get[Option[Int]]("familles_husb") ~
      get[Option[Instant]]("familles_timestamp") ~
      get[String]("familles_refn") ~
      get[String]("familles_refn_type") ~
      get[Int]("base")).map {
      case id ~ wife ~ husb ~ timestamp ~ refn ~ refnType ~ base =>
        FamilyQueryData(id, husb, wife, timestamp.getOrElse(Instant.now), None, refn, refnType, base)
    }
}
