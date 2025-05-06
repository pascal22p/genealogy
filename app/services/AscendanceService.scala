package services

import javax.inject.Inject

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.data.OptionT
import cats.implicits.*
import models.EventType.FamilyEvent
import models.Events
import models.Family
import models.Parents
import models.Person

class AscendanceService @Inject() (personService: PersonService)(implicit val ec: ExecutionContext) {

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def getAscendant(id: Int, depth: Int = 1): Future[Option[Person]] = {
    personService
      .getPerson(id, omitSources = true, omitFamilies = true)
      .flatMap { (personOption: Option[Person]) =>
        personOption.traverse { person =>
          val parents: Future[List[Parents]] = person.parents.traverse { parent =>
            for {
              parent1 <- parent.family.parent1.traverse { parent =>
                getAscendant(parent.details.id, depth + 1)
              }
              parent2 <- parent.family.parent2.traverse { parent =>
                getAscendant(parent.details.id, depth + 1)
              }
            } yield {
              Parents(
                Family(
                  parent.family.id,
                  parent1.flatten,
                  parent2.flatten,
                  parent.family.timestamp,
                  parent.family.privacyRestriction,
                  parent.family.refn,
                  List.empty,
                  Events(List.empty, Some(parent.family.id), FamilyEvent)
                ),
                parent.refnType,
                parent.relaType,
                parent.relaStat
              )
            }
          }
          parents.map(a => person.copy(parents = a))
        }
      }
  }

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def buildSosaList(personId: Int, sosaNumber: Int = 1, maxDepth: Int = 6): Future[Map[Int, Person]] = {
    personService
      .getPerson(personId, omitSources = true, omitFamilies = true, omitParents = false)
      .flatMap {
        case None => Future.successful(Map.empty[Int, Person])

        case Some(person) =>
          val current    = Map(sosaNumber -> person)
          val generation = sosaNumber.toBinaryString.length - 1

          if (generation >= maxDepth - 1) {
            Future.successful(current)
          } else {

            val maybeParent1: OptionT[Future, Map[Int, Person]] = for {
              parents <- OptionT.fromOption[Future](person.parents.headOption)
              parent1 <- OptionT.fromOption[Future](parents.family.parent1)
              result1 <- OptionT(buildSosaList(parent1.details.id, sosaNumber * 2).map(x => Some(x)))
            } yield {
              result1
            }

            val maybeParent2: OptionT[Future, Map[Int, Person]] = for {
              parents <- OptionT.fromOption[Future](person.parents.headOption)
              parent2 <- OptionT.fromOption[Future](parents.family.parent2)
              result2 <- OptionT(buildSosaList(parent2.details.id, sosaNumber * 2 + 1).map(x => Some(x)))
            } yield {
              result2
            }

            for {
              p1 <- maybeParent1.value
              p2 <- maybeParent2.value
            } yield {
              current ++ p1.getOrElse(Map.empty[Int, Person]) ++ p2.getOrElse(Map.empty[Int, Person])
            }
          }
      }
  }

}
