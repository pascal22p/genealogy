package models.forms.extensions

import play.api.data.Form

object FillFormExtension {

  extension [A](form: Form[A]) {
    def filledWith(values: Option[A]): Form[A] = {
      values match {
        case Some(value) => form.fill(value)
        case None        => form
      }
    }
  }

}
