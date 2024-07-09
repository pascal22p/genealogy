package models

import anorm._
import anorm.SqlParser._

case class Place (
                   id: Int,
                   lieuDit: String,
                   city: String,
                   postCode: String,
                   inseeNumber: Option[Int],
                   county: String,
                   region: String,
                   country: String,
                   longitude: Option[Double],
                   latitude: Option[Double],
                   base: Int
                 ) {
  def oneLiner: String = {
    List(lieuDit, city, postCode, county, region, country).flatMap { el =>
      if(el.trim == "") {
        None
      } else {
        Some(el)
      }
    }.mkString(", ")
  }
}

object Place {
  val mysqlParser: RowParser[Place] =
    get[Int]("place_id") ~
      get[Option[String]]("place_lieudit") ~
      get[String]("place_ville") ~
      get[String]("place_cp") ~
      get[Option[Int]]("place_insee") ~
      get[Option[String]]("place_departement") ~
      get[String]("place_region") ~
      get[String]("place_pays") ~
      get[Option[Double]]("place_longitude") ~
      get[Option[Double]]("place_latitude") ~
      get[Int]("base") map {
      case id ~ lieuDit ~ city ~ postCode ~ inseeNumber ~ county ~ region ~ country ~ longitude ~ latitude ~ base =>
        Place(id, lieuDit.getOrElse(""), city, postCode, inseeNumber, county.getOrElse(""), region, country, longitude, latitude, base)
    }
}