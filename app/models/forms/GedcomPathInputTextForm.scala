package models.forms

import java.nio.file.Files
import java.nio.file.Paths

import models.journeyCache.UserAnswersItem
import play.api.data.*
import play.api.data.validation.*
import play.api.data.Forms.*

final case class GedcomPathInputTextForm(selectedFile: String) extends UserAnswersItem

object GedcomPathInputTextForm {
  def unapply(
      u: GedcomPathInputTextForm
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

  def form(basePath: String): Form[GedcomPathInputTextForm] = Form(
    mapping(
      "selectedFile" -> nonEmptyText.verifying(validGedcomPath(basePath))
    )(GedcomPathInputTextForm.apply)(GedcomPathInputTextForm.unapply)
  )

}
