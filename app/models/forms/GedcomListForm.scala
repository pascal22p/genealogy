package models.forms

import java.nio.file.Files
import java.nio.file.Paths

import play.api.data.*
import play.api.data.validation._
import play.api.data.Forms.*

final case class GedcomListForm(selectedFile: String) extends CaseClassForms

object GedcomListForm {
  def unapply(
      u: GedcomListForm
  ): Some[String] = Some(
    u.selectedFile
  )

  private def validGedcomPath(uploadPath: String): Constraint[String] =
    Constraint("constraints.gedcomPath") { selectedFile =>
      val basePath = Paths.get(uploadPath)
      val sanitise = s"./${basePath.resolve(selectedFile).normalize()}"

      if (!sanitise.startsWith(s"$basePath")) {
        Invalid(ValidationError("Invalid file path"))
      } else if (!Files.exists(Paths.get(sanitise))) {
        Invalid(ValidationError("File does not exist"))
      } else {
        Valid
      }
    }

  def form(basePath: String): Form[GedcomListForm] = Form(
    mapping(
      "selectedFile" -> nonEmptyText.verifying(validGedcomPath(basePath))
    )(GedcomListForm.apply)(GedcomListForm.unapply)
  )

}
