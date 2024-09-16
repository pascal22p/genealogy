package services

import javax.inject.Inject

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.implicits.*
import models.Person

class DescendanceService @Inject() (personService: PersonService)(implicit val ec: ExecutionContext) {

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def getDescendant(id: Int, depth: Int): Future[Option[Person]] = {
    personService
      .getPerson(id, omitSources = true, omitParents = true)
      .flatMap { (personOption: Option[Person]) =>
        personOption.traverse { person =>
          val listFamilyFuture = person.families.traverse { family =>
            val listChildFuture = family.children.traverse { child =>
              getDescendant(child.person.details.id, depth + 1).map {
                case Some(personBelow) => child.copy(person = personBelow)
                case None              => child
              }
            }
            listChildFuture.map { listChild =>
              family.copy(children = listChild)
            }
          }
          listFamilyFuture.map { listFamily =>
            person.copy(families = listFamily)
          }
        }
      }
  }

}
