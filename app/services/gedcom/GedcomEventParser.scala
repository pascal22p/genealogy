package services.gedcom

import cats.data.Ior
import com.google.inject.Inject
import com.google.inject.Singleton
import models.gedcom.GedcomEventBlock
import models.gedcom.GedcomNode

@Singleton
class GedcomEventParser @Inject() () {

  def readEventBlock(node: GedcomNode): Ior[List[String], GedcomEventBlock] = {
    val eventTag  = node.name
    val eventDate = node.children.find(_.name == "DATE").flatMap(_.content)

    Ior.Right(GedcomEventBlock(eventTag, eventDate.getOrElse("")))
  }
}
