package models.forms

import models.journeyCache.UserAnswersItem
import models.GenealogyDatabase
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.text

final case class NewDatabaseForm(
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

object NewDatabaseForm {

  def unapply(
      u: NewDatabaseForm
  ): Some[(String, String)] = Some(
    (
      u.name,
      u.description
    )
  )

  val databaseForm: Form[NewDatabaseForm] = Form(
    mapping(
      "name"        -> text,
      "description" -> text
    )(NewDatabaseForm.apply)(NewDatabaseForm.unapply)
  )
}
