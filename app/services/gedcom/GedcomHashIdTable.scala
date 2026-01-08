package services.gedcom

import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

import scala.collection.concurrent.TrieMap

@Singleton
class GedcomHashIdTable @Inject() () {

  private val individualHashMap: TrieMap[String, TrieMap[String, Int]] = TrieMap[String, TrieMap[String, Int]]()
  private val familyHashMap: TrieMap[String, TrieMap[String, Int]]     = TrieMap[String, TrieMap[String, Int]]()
  private val placeHashMap: TrieMap[String, TrieMap[String, Int]]      = TrieMap[String, TrieMap[String, Int]]()
  private val jobInProgress: TrieMap[String, Seq[(Instant, String)]]   = TrieMap[String, Seq[(Instant, String)]]()

  private val individualCounter: TrieMap[String, AtomicInteger] = TrieMap[String, AtomicInteger]()
  private val familyCounter: TrieMap[String, AtomicInteger]     = TrieMap[String, AtomicInteger]()
  private val placeCounter: TrieMap[String, AtomicInteger]      = TrieMap[String, AtomicInteger]()
  private val eventCounter: TrieMap[String, AtomicInteger]      = TrieMap[String, AtomicInteger]()

  def clearAllData(jobId: String): Unit = {
    individualHashMap.get(jobId).foreach(_.clear())
    familyHashMap.get(jobId).foreach(_.clear())
    placeHashMap.get(jobId).foreach(_.clear())
    jobInProgress.remove(jobId)

    individualCounter.remove(jobId)
    familyCounter.remove(jobId)
    placeCounter.remove(jobId)
    ()
  }

  def getJobStatus(jobId: String): Seq[(Instant, String)] =
    jobInProgress.getOrElse(jobId, Seq.empty)

  def updateJobStatus(jobId: String, newStatus: String): Seq[(Instant, String)] =
    jobInProgress
      .updateWith(jobId) {
        case Some(history) => Some((Instant.now, newStatus) +: history)
        case None          => Some(Seq((Instant.now, newStatus)))
      }
      .getOrElse(Seq.empty)

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  def getIndividualIdFromString(jobId: String, stringId: String): Int = {
    val map = individualHashMap.getOrElseUpdate(jobId, TrieMap.empty)
    map
      .updateWith(stringId) {
        case Some(existing) => Some(existing)
        case None           =>
          val id =
            individualCounter
              .getOrElseUpdate(jobId, AtomicInteger(-1))
              .incrementAndGet()
          Some(id)
      }
      .get
  }

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  def getFamilyIdFromString(jobId: String, stringId: String): Int = {
    val map = familyHashMap.getOrElseUpdate(jobId, TrieMap.empty)
    map
      .updateWith(stringId) {
        case Some(existing) => Some(existing)
        case None           =>
          val id =
            familyCounter
              .getOrElseUpdate(jobId, AtomicInteger(-1))
              .incrementAndGet()
          Some(id)
      }
      .get
  }

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  def getPlaceIdFromString(jobId: String, stringId: String): Int = {
    val map = placeHashMap.getOrElseUpdate(jobId, TrieMap.empty)
    map
      .updateWith(stringId) {
        case Some(existing) => Some(existing)
        case None           =>
          val id =
            placeCounter
              .getOrElseUpdate(jobId, AtomicInteger(-1))
              .incrementAndGet()
          Some(id)
      }
      .get
  }

  def getEventId(jobId: String): Int =
    eventCounter
      .getOrElseUpdate(jobId, AtomicInteger(-1))
      .incrementAndGet()
}
