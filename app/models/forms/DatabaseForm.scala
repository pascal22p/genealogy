package models.forms

import java.time.Instant

import anorm._
import anorm.SqlParser._
import models.MaleSex
import models.GenealogyDatabase
import models.Sex
import play.api.data.format.Formats._
import play.api.data.FieldMapping
import play.api.data.Form
import play.api.data.Forms.ignored
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.data.Forms.optional
import play.api.data.Forms.text

final case class DatabaseForm(
    name: String,
    description: String
) {
  def toGenealogyDatabase: GenealogyDatabase = {
    GenealogyDatabase(
      0,
      name,
      description
    )
  }

}

object DatabaseForm {

  def unapply(
      u: DatabaseForm
  ): Some[(String, String)] = Some(
    (
      u.name,
      u.description
    )
  )

  val databaseForm: Form[DatabaseForm] = Form(
    mapping(
      "name"          -> text,
      "description"            -> text
    )(DatabaseForm.apply)(DatabaseForm.unapply)
  )
}
