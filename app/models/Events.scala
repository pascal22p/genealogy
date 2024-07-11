package models

import play.api.i18n.Messages

final case class Events(eventsDetails: List[EventDetail]) {
  def birthAndDeathDate(implicit messages: Messages): String = {
    val birthTags = List("BIRT", "BAPM")
    val birthDate = eventsDetails.find(event => birthTags.contains(event.tag)).map(_.formatDate)
    val deathTags = List("DEAT", "BURI")
    val deathDate = eventsDetails.find(event => deathTags.contains(event.tag)).map(_.formatDate)
    (birthDate, deathDate) match {
      case (None, None)               => ""
      case (Some(date), None)         => s"(°$date)"
      case (None, Some(date))         => s"(†$date)"
      case (Some(date1), Some(date2)) => s"(°$date1 – †$date2)"
    }
  }
}
