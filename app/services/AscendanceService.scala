package services

import javax.inject.Inject

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

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

}
