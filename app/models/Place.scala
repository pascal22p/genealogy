package models

import anorm._
import anorm.SqlParser._
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent

final case class Place(
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
  def oneLinerString: String = {
    List(lieuDit, city, postCode, country)
      .flatMap { el =>
        if (el.trim == "") {
          None
        } else {
          Some(el)
        }
      }
      .mkString(", ")
  }

  def oneLiner(short: Boolean = true): Html = {
    if (short) {
      val placeString = List(lieuDit, city, postCode, country)
        .flatMap { el =>
          if (el.trim == "") {
            None
          } else {
            Some(el)
          }
        }
        .mkString(", ")
      Html(s"""<p class="govuk-body">$placeString</p>""")
    } else {
      val location = (latitude, longitude) match {
        case (Some(latitude), Some(longitude)) =>
          s"""<p class="govuk-body"><a class="govuk-link" href="https://www.google.com/maps/search/?api=1&query=$latitude,$longitude">location: ($latitude, $longitude)</a></p>"""
        case _ => ""
      }
      val placeString = List(lieuDit, city, postCode, county, region, country)
        .flatMap { el =>
          if (el.trim == "") {
            None
          } else {
            Some(el)
          }
        }
        .mkString(", ")
      Html(s"""<p class="govuk-body">$placeString</p>$location""")
    }
  }
}

object Place {
  val mysqlParser: RowParser[Place] =
    (get[Int]("place_id") ~
      get[Option[String]]("place_lieudit") ~
      get[String]("place_ville") ~
      get[String]("place_cp") ~
      get[Option[Int]]("place_insee") ~
      get[Option[String]]("place_departement") ~
      get[String]("place_region") ~
      get[String]("place_pays") ~
      get[Option[Double]]("place_longitude") ~
      get[Option[Double]]("place_latitude") ~
      get[Int]("base")).map {
      case id ~ lieuDit ~ city ~ postCode ~ inseeNumber ~ county ~ region ~ country ~ longitude ~ latitude ~ base =>
        Place(
          id,
          lieuDit.getOrElse(""),
          city,
          postCode,
          inseeNumber,
          county.getOrElse(""),
          region,
          country,
          longitude,
          latitude,
          base
        )
    }
}
