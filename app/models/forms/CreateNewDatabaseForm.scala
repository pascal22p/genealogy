package models.forms

import models.journeyCache.UserAnswersItem
import models.GenealogyDatabase
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.text

final case class CreateNewDatabaseForm(
    name: String,
    description: String
) extends UserAnswersItem {
  def toGenealogyDatabase: GenealogyDatabase = {
    GenealogyDatabase(
      0,
      name,
      description,
      None
    )
  }

}

object CreateNewDatabaseForm {

  def unapply(
      u: CreateNewDatabaseForm
  ): Some[(String, String)] = Some(
    (
      u.name,
      u.description
    )
  )

  val databaseForm: Form[CreateNewDatabaseForm] = Form(
    mapping(
      "name"        -> text,
      "description" -> text
    )(CreateNewDatabaseForm.apply)(CreateNewDatabaseForm.unapply)
  )
}
