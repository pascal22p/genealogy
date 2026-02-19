package models.queryData

import anorm._
import anorm.RowParser
import anorm.SqlParser._

final case class AddressQueryData(
    id: Int,

    address: String,
    city: String,
    state: String,
    postcode: String,
    country: String,

    phone1: String,
    phone2: String,
    phone3: String,

    email1: String,
    email2: String,
    email3: String,

    fax1: String,
    fax2: String,
    fax3: String,

    website1: String,
    website2: String,
    website3: String
) {
  def oneLiner: String = s"$address, $city, $postcode, $country"
}

object AddressQueryData {
  def apply(id: Int) = new AddressQueryData(id, "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")

  def apply(
      id: Int,

      address: String,
      city: String,
      state: String,
      postcode: String,
      country: String,

      phone1: String,
      phone2: String,
      phone3: String,

      email1: String,
      email2: String,
      email3: String,

      fax1: String,
      fax2: String,
      fax3: String,

      website1: String,
      website2: String,
      website3: String
  ) = new AddressQueryData(
    id,

    address,
    city,
    state: String,
    postcode: String,
    country: String,

    phone1: String,
    phone2: String,
    phone3: String,

    email1: String,
    email2: String,
    email3: String,

    fax1: String,
    fax2: String,
    fax3: String,

    website1: String,
    website2: String,
    website3: String
  )

  val mysqlParserAddress: RowParser[AddressQueryData] =
    (get[Int]("addr_id") ~
      get[String]("addr_addr") ~
      get[String]("addr_city") ~
      get[String]("addr_stae") ~
      get[String]("addr_post") ~
      get[String]("addr_ctry") ~

      get[String]("addr_phon1") ~
      get[String]("addr_phon2") ~
      get[String]("addr_phon3") ~

      get[String]("addr_email1") ~
      get[String]("addr_email2") ~
      get[String]("addr_email3") ~

      get[String]("addr_fax1") ~
      get[String]("addr_fax2") ~
      get[String]("addr_fax3") ~

      get[String]("addr_www1") ~
      get[String]("addr_www2") ~
      get[String]("addr_www3"))
      .map {
        case id ~
            address ~ city ~ state ~ postcode ~ country ~
            phone1 ~ phone2 ~ phone3 ~
            email1 ~ email2 ~ email3 ~
            fax1 ~ fax2 ~ fax3 ~
            website1 ~ website2 ~ website3 =>

          AddressQueryData(
            id = id,
            address = address,
            city = city,
            state = state,
            postcode = postcode,
            country = country,
            phone1 = phone1,
            phone2 = phone2,
            phone3 = phone3,
            email1 = email1,
            email2 = email2,
            email3 = email3,
            fax1 = fax1,
            fax2 = fax2,
            fax3 = fax3,
            website1 = website1,
            website2 = website2,
            website3 = website3
          )
      }

}
