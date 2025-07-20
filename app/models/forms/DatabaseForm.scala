package models.forms

import models.GenealogyDatabase
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.text

final case class DatabaseForm(
    name: String,
    description: String,
    medias: String
) {
  def toGenealogyDatabase: GenealogyDatabase = {
    GenealogyDatabase(
      0,
      name,
      description,
      Some(medias)
    )
  }

}

object DatabaseForm {

  def unapply(
      u: DatabaseForm
  ): Some[(String, String, String)] = Some(
    (
      u.name,
      u.description,
      u.medias
    )
  )

  val databaseForm: Form[DatabaseForm] = Form(
    mapping(
      "name"        -> text,
      "description" -> text,
      "medias"      -> text
    )(DatabaseForm.apply)(DatabaseForm.unapply)
  )
}
