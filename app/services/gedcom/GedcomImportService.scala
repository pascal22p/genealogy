package services.gedcom

import javax.inject.Inject
import javax.inject.Singleton

import cats.data.Ior
import models.gedcom.GedcomIndiBlock
import models.gedcom.GedcomNode

@Singleton
class GedcomImportService @Inject() (
    gedcomCommonParser: GedcomCommonParser,
    gedcomIndividualParser: GedcomIndividualParser
) {

  def gedcom2sql(gedcomString: String) = {
    val nodes = gedcomCommonParser.getTree(gedcomString)
    convertTree(nodes, 3)
    true
  }

  def convertTree(nodes: List[GedcomNode], base: Int) = {
    val indis: Ior[List[String], List[GedcomIndiBlock]] = nodes
      .filter(_.name == "INDI")
      .map(gedcomIndividualParser.readIndiBlock)
      .foldLeft(Ior.Right(List.empty): Ior[List[String], List[GedcomIndiBlock]]) {
        case (listIndividuals, individual) =>
          listIndividuals.combine(individual.map(i => List(i)))
      }

    val warnings = indis.left.getOrElse(List.empty)
    warnings.foreach { warning =>
      println(warning)
    }

    val sqlHead =
      s"INSERT INTO genea_individuals (indi_id, base, indi_nom, indi_prenom, indi_sexe, indi_npfx, indi_givn, indi_nick, indi_spfx, indi_nsfx, indi_resn) VALUES\n"
    val indiSqls = indis.right
      .getOrElse(List.empty)
      .map { indi =>
        val nameRegex = "([^/]*)/([^/]*)/(.*)".r
        val (firstname, surname) = indi.nameStructure.name match {
          case nameRegex(firstname, surname, other) => (firstname, surname + " " + other)
          case _                                    => ("", "")
        }
        s"""(${gedcomCommonParser.getIndiId(indi.id)}, $base, "$surname", "$firstname", "${indi.sex}", "${indi.nameStructure.npfx}", "${indi.nameStructure.givn}", "${indi.nameStructure.nick}", "${indi.nameStructure.spfx}", "${indi.nameStructure.nsfx}", ${indi.resn})"""
      }
      .mkString(sqlHead, ",\n", "")

    println(indiSqls)
  }

}
