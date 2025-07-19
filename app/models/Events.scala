package models

import config.AppConfig
import play.api.i18n.Messages
import play.twirl.api.Html

final case class Events(eventsDetails: List[EventDetail], ownerId: Option[Int], ownerType: EventType.EventType)
    extends EventsOrAttributes {

  def birthAndDeathDate(shortMonth: Boolean = false)(
      implicit messages: Messages,
      authenticatedRequest: AuthenticatedRequest[?],
      appConfig: AppConfig
  ): Html = {
    val birthTags = List("BIRT", "BAPM")
    val birthDate =
      eventsDetails.find(event => birthTags.contains(event.tag.getOrElse(""))).map(_.formatDate(shortMonth))
    val deathTags = List("DEAT", "BURI")
    val deathDate =
      eventsDetails.find(event => deathTags.contains(event.tag.getOrElse(""))).map(_.formatDate(shortMonth))
    (birthDate, deathDate) match {
      case (None, None)               => Html("")
      case (Some(date), None)         => Html(s"°$date")
      case (None, Some(date))         => Html(s"†$date")
      case (Some(date1), Some(date2)) => Html(s"°$date1 – †$date2")
    }
  }

  def weddingDate(shortMonth: Boolean = false)(
      implicit messages: Messages,
      authenticatedRequest: AuthenticatedRequest[?],
      appConfig: AppConfig
  ): Html = {
    val weddingTags = List("MARR", "MARB")
    val weddingDate =
      eventsDetails.find(event => weddingTags.contains(event.tag.getOrElse(""))).map(_.formatDate(shortMonth))
    weddingDate.fold(Html(""))(date => Html(s"x $date"))
  }

}
