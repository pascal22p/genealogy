package models

import config.AppConfig
import play.api.i18n.Messages
import play.twirl.api.Html
import utils.GedcomDateLibrary

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
    Html(GedcomDateLibrary.birthAndDeathDate(birthDate, deathDate, shortMonth))
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
