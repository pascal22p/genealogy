package models.queryData

import anorm._
import anorm.SqlParser._
import java.time.Instant

final case class FamilyQueryData(
  id: Int, 
  parent1: Option[Int], 
  parent2: Option[Int], 
  timestamp: Instant, 
  privacyRestriction: Option[String], 
  refn: String,
  refnType: String
  )

object FamilyQueryData {
    val mysqlParser: RowParser[FamilyQueryData] =
    get[Int]("familles_id") ~
      get[Option[Int]]("familles_wife") ~
      get[Option[Int]]("familles_husb") ~
      get[Option[Instant]]("familles_timestamp") ~
      get[String]("familles_refn") ~
      get[String]("familles_refn_type") map {
      case id ~ wife ~ husb ~ timestamp ~ refn ~ refnType =>
        FamilyQueryData(id, husb, wife, timestamp.getOrElse(Instant.now), None, refn, refnType)
    }
}