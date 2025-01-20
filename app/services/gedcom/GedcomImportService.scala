package services.gedcom

import anorm.SQL

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

    val startTransaction =
      """
        |START TRANSACTION;
        |select * from genea_individuals LOCK IN SHARE MODE;
        |SELECT `AUTO_INCREMENT` INTO @startIndi FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'genealogie' AND TABLE_NAME = 'genea_individuals';
        |""".stripMargin

    val commitTransaction = "COMMIT;"

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
        SQL("""$sqlHead ({indi_id} + @startIndi, {base}, {surname}, {firstname}, {indi_sex}, "${indi.nameStructure.npfx}", "${indi.nameStructure.givn}", "${indi.nameStructure.nick}", "${indi.nameStructure.spfx}", "${indi.nameStructure.nsfx}", ${indi.resn.getOrElse("NULL")});""")
          .on("indi_id" -> indi.id,
            "base" -> base,
            "surname" -> surname,
            "firstname" -> firstname,


          )
      }

    println(startTransaction + indiSqls + commitTransaction)
  }

}
