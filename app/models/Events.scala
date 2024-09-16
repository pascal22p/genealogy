package models

import play.api.i18n.Messages
import play.twirl.api.Html

final case class Events(eventsDetails: List[EventDetail]) {
  def birthAndDeathDate(implicit messages: Messages): Html = {
    val birthTags = List("BIRT", "BAPM")
    val birthDate = eventsDetails.find(event => birthTags.contains(event.tag.getOrElse(""))).map(_.formatDate)
    val deathTags = List("DEAT", "BURI")
    val deathDate = eventsDetails.find(event => deathTags.contains(event.tag.getOrElse(""))).map(_.formatDate)
    (birthDate, deathDate) match {
      case (None, None)               => Html("")
      case (Some(date), None)         => Html(s"<span style=\"font-size:small\">(°$date)</span>")
      case (None, Some(date))         => Html(s"<span style=\"font-size:small\">(†$date)</span>")
      case (Some(date1), Some(date2)) => Html(s"<span style=\"font-size:small\">(°$date1 – †$date2)</span>")
    }
  }
}
