package services

import javax.inject.Inject
import javax.inject.Singleton

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.implicits.*
import config.AppConfig
import models.EventType.UnknownEvent
import models.Events
import models.Family
import models.Person
import models.ResnType.PrivacyResn
import play.api.Logging

final case class Tree(
    individuals: mutable.Map[Int, Person],
    families: mutable.Map[Int, Family],
    links: mutable.Set[(String, String)],
    groups: mutable.Map[Int, Set[Int]]
) {
  def addPerson(person: Person): Unit = {
    individuals.put(person.details.id, person.copy(families = List.empty, parents = List.empty)): Unit
  }

  def addFamily(family: Family): Unit = {
    families.put(family.id, family): Unit
  }

  def addLink(source: String, destination: String): Unit = {
    links.add((source, destination)): Unit
  }

  def addGroup(id: Int, indis: Set[Int]): Unit = {
    groups.put(id, indis): Unit
  }
}

object Tree {
  def empty: Tree = Tree(mutable.Map.empty, mutable.Map.empty, mutable.Set.empty, mutable.Map.empty)
}

@Singleton
class TreeService @Inject() (personService: PersonService, familyService: FamilyService, appConfig: AppConfig)(
    implicit ec: ExecutionContext
) extends Logging {
  def loadTree(id: Int, depth: Int = 0, maxDepth: Int = 2, tree: Tree, isAllowedToSee: Boolean): Future[Boolean] = {
    personService.getPerson(id, omitSources = true).flatMap {
      case Some(person) if tree.individuals.contains(person.details.id) =>
        Future.successful(true)
      case Some(person) if depth == maxDepth || depth == -maxDepth =>
        logger.info(s"Reached max depth for person with ID ${person.details.id}, skipping.")
        tree.addPerson(person)
        Future.successful(true)
      case Some(person) if person.details.privacyRestriction.contains(PrivacyResn) && !isAllowedToSee =>
        tree.addPerson(
          person.copy(
            details = person.details.copy(firstname = appConfig.redactedMask, surname = appConfig.redactedMask),
            events = Events(List.empty, None, UnknownEvent)
          )
        )
        Future.successful(true)
      case Some(person) =>
        tree.addPerson(person)

        val familiesFuture = person.families.traverse { family =>
          for {
            _ <- family.parent1.traverse { parent =>
              if (person.details.id == parent.details.id) {
                Future.successful(tree.addLink(s"I${parent.details.id}", s"F${family.id}"))
              } else {
                loadTree(parent.details.id, depth, maxDepth, tree, isAllowedToSee).map {
                  case true => tree.addLink(s"I${parent.details.id}", s"F${family.id}")
                  case _    => ()
                }
              }
            }

            _ <- family.parent2.traverse { parent =>
              if (person.details.id == parent.details.id) {
                Future.successful(tree.addLink(s"I${parent.details.id}", s"F${family.id}"))
              } else {
                loadTree(parent.details.id, depth, maxDepth, tree, isAllowedToSee).map {
                  case true  => tree.addLink(s"I${parent.details.id}", s"F${family.id}")
                  case false => ()
                }
              }
            }

            _ <- family.children.traverse { child =>
              if (person.details.id == child.person.details.id) {
                Future.successful(tree.addLink(s"F${family.id}", s"I${child.person.details.id}"))
              } else {
                loadTree(child.person.details.id, depth + 1, maxDepth, tree, isAllowedToSee).map { _ =>
                  tree.addLink(s"F${family.id}", s"I${child.person.details.id}")
                }
              }
            }

          } yield {
            tree.addFamily(family)
            tree.addGroup(family.id, Set(family.parent1.map(_.details.id), family.parent2.map(_.details.id)).flatten)
          }
        }

        val parentsFuture = person.parents
          .traverse { parents =>
            for {
              // partner link to family
              _ <- parents.family.parent1.map { parent =>
                loadTree(parent.details.id, depth - 1, maxDepth, tree, isAllowedToSee).map { _ =>
                  tree.addLink(s"I${parent.details.id}", s"F${parents.family.id}")
                }
              }.sequence

              _ <- parents.family.parent2.map { parent =>
                loadTree(parent.details.id, depth - 1, maxDepth, tree, isAllowedToSee).map { _ =>
                  tree.addLink(s"I${parent.details.id}", s"F${parents.family.id}")
                }
              }.sequence

              familyDetails <- familyService.getFamilyDetails(parents.family.id, omitSources = true).value
            } yield {
              familyDetails.foreach { family =>
                tree.addFamily(family)
              }
              tree.addLink(s"F${parents.family.id}", s"I${person.details.id}")
              tree.addGroup(
                parents.family.id,
                Set(parents.family.parent1.map(_.details.id), parents.family.parent2.map(_.details.id)).flatten
              )
            }
          }

        for {
          _ <- familiesFuture
          _ <- parentsFuture
        } yield {
          true
        }

      case None => Future.successful(false)
    }
  }
}
