package services.gedcom

import com.google.inject.Inject
import com.google.inject.Singleton

@Singleton
class GedcomHashIdTable @Inject() () {
  private val individualHashMap: scala.collection.mutable.Map[String, Int] =
    scala.collection.mutable.Map.empty[String, Int]

  private def insertNewIndividualId(stringId: String): Int = {
    val nextId: Int = individualHashMap.values.maxOption.fold(0)(_ + 1)
    individualHashMap += (stringId -> nextId)
    nextId
  }

  def getIndividualIdFromString(stringId: String): Int = {
    individualHashMap.getOrElse(stringId, insertNewIndividualId(stringId))
  }

  private var LastEventId: Option[Int] = None

  def getEventId: Int = {
    val nextId: Int = LastEventId.fold(0)(_ + 1)
    LastEventId = Some(nextId)
    nextId
  }
}
