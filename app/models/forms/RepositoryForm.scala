package models.forms

import models.queryData.AddressQueryData
import models.queryData.RepositoryQueryData
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.nonEmptyText
import play.api.data.Forms.number
import play.api.data.Forms.optional
import play.api.data.Forms.text

final case class RepositoryForm(
    repoName: String,
    repoRin: String,
    addrId: Option[Int]
) {
  def toRepositoryQueryData(dbId: Int) =
    RepositoryQueryData(-1, dbId, repoName, repoRin, addrId.map(id => AddressQueryData.apply(id)))
}

object RepositoryForm {
  def unapply(
      u: RepositoryForm
  ): Some[(String, String, Option[Int])] = Some(
    (u.repoName, u.repoRin, u.addrId)
  )

  val repositoryForm: Form[RepositoryForm] = Form(
    mapping(
      "repoName" -> nonEmptyText(maxLength = 90),
      "repoRin"  -> text(maxLength = 255),
      "addrId"   -> optional(number)
    )(RepositoryForm.apply)(RepositoryForm.unapply)
  )
}
