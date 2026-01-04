package services.gedcom

import com.google.inject.Inject
import com.google.inject.Singleton

@Singleton
class GedcomHashIdTable @Inject() () {
  private val individualHashMap: scala.collection.mutable.Map[String, Int] =
    scala.collection.mutable.Map.empty[String, Int]
  private val familyHashMap: scala.collection.mutable.Map[String, Int] =
    scala.collection.mutable.Map.empty[String, Int]
  val placeHashMap: scala.collection.mutable.Map[String, Int] =
    scala.collection.mutable.Map.empty[String, Int]

  def clearAllData: Unit = {
    individualHashMap.clear()
    familyHashMap.clear()
    placeHashMap.clear()
    println(s"^^^^^^^^^ ${individualHashMap.size}, ${familyHashMap.size}, ${placeHashMap.size}")
  }

  private def insertNewIndividualId(stringId: String): Int = {
    val nextId: Int = individualHashMap.values.maxOption.fold(0)(_ + 1)
    individualHashMap += (stringId -> nextId)
    nextId
  }

  def getIndividualIdFromString(stringId: String): Int = {
    individualHashMap.getOrElse(stringId, insertNewIndividualId(stringId))
  }

  private def insertNewFamilyId(stringId: String): Int = {
    val nextId: Int = familyHashMap.values.maxOption.fold(0)(_ + 1)
    familyHashMap += (stringId -> nextId)
    nextId
  }

  def getFamilyIdFromString(stringId: String): Int = {
    familyHashMap.getOrElse(stringId, insertNewFamilyId(stringId))
  }

  private def insertNewPlaceId(stringId: String): Int = {
    val nextId: Int = placeHashMap.values.maxOption.fold(0)(_ + 1)
    placeHashMap += (stringId -> nextId)
    nextId
  }

  def getPlaceIdFromString(stringId: String): Int = {
    placeHashMap.getOrElse(stringId, insertNewPlaceId(stringId))
  }

  private var LastEventId: Option[Int] = None
  def getEventId: Int                  = {
    val nextId: Int = LastEventId.fold(0)(_ + 1)
    LastEventId = Some(nextId)
    nextId
  }
}
