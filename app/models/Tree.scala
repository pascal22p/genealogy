package models

import scala.collection.mutable

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
