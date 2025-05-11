package models.forms

import models.GenealogyDatabase
import play.api.data.Form
import play.api.data.Forms.mapping
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
      "name"        -> text,
      "description" -> text
    )(DatabaseForm.apply)(DatabaseForm.unapply)
  )
}
