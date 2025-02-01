package utils

import javax.inject.Inject
import javax.inject.Singleton

import models.Parents
import models.Person

@Singleton
class TreeUtils @Inject() () {

  def mergeMaps(map1: Map[Int, List[Person]], map2: Map[Int, List[Person]]): Map[Int, List[Person]] = {
    val mergedList = map1.toList ++ map2.toList
    val keys       = mergedList.map(_._1)
    keys.distinct.map { key =>
      val values = mergedList.filter(_._1 == key).foldLeft(List.empty[Person]) {
        case (all: List[Person], current: (Int, List[Person])) =>
          all ++ current._2
      }
      key -> values
    }.toMap
  }

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def flattenTree(
      personTree: Person,
      depth: Int = 1
  ): Map[Int, List[Person]] = {
    val allParents: List[Person] = personTree.parents.flatMap { parent =>
      List(parent.family.parent1, parent.family.parent2).flatten
    }
    if (allParents.isEmpty) {
      Map.empty[Int, List[Person]]
    } else {
      val flatMapTree: Map[Int, List[Person]] = Map(
        depth -> allParents.map(_.copy(parents = List.empty[Parents]))
      )

      allParents.foldLeft(flatMapTree) {
        case (all, current) =>
          mergeMaps(all, flattenTree(current, depth + 1))
      }
    }
  }

  def deduplicate(tree: Map[Int, List[Person]]): Map[Int, List[Person]] = {
    tree.map {
      case (depth, persons) =>
        depth -> persons.distinct
    }
  }

}
