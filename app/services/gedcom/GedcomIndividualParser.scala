package services.gedcom

import javax.inject.Inject

import anorm.BatchSql
import anorm.NamedParameter
import cats.data.Ior
import cats.implicits.toTraverseOps
import com.google.inject.Singleton
import models.gedcom.GedComPersonalNameStructure
import models.gedcom.GedcomEventBlock
import models.gedcom.GedcomIndiBlock
import models.gedcom.GedcomNode
import models.ResnType
import models.ResnType.ResnType
import utils.Constants

@Singleton
class GedcomIndividualParser @Inject() (
    gedcomCommonParser: GedcomCommonParser,
    gedcomHashIdTable: GedcomHashIdTable,
    gedcomEventParser: GedcomEventParser
) {

  def readIndiBlock(node: GedcomNode, jobId: String): Ior[Seq[String], GedcomIndiBlock] = {
    /*
    @XREF:INDI@ INDI {1:1}
        +1 RESN <RESTRICTION_NOTICE> {0:1} p.60
        +1 <<PERSONAL_NAME_STRUCTURE>> {0:M} p.38
        +1 SEX <SEX_VALUE> {0:1} p.61
        +1 <<INDIVIDUAL_EVENT_STRUCTURE>> {0:M} p.34
        +1 <<INDIVIDUAL_ATTRIBUTE_STRUCTURE>> {0:M} p.33
        +1 <<LDS_INDIVIDUAL_ORDINANCE>> {0:M} p.35, 36
        +1 <<CHILD_TO_FAMILY_LINK>> {0:M} p.31
        +1 <<SPOUSE_TO_FAMILY_LINK>> {0:M} p.40
        +1 SUBM @<XREF:SUBM>@ {0:M} p.28
        +1 <<ASSOCIATION_STRUCTURE>> {0:M} p.31
        +1 ALIA @<XREF:INDI>@ {0:M} p.25
        +1 ANCI @<XREF:SUBM>@ {0:M} p.28
        +1 DESI @<XREF:SUBM>@ {0:M} p.28
        +1 RFN <PERMANENT_RECORD_FILE_NUMBER> {0:1} p.57
        +1 AFN <ANCESTRAL_FILE_NUMBER> {0:1} p.42
        +1 REFN <USER_REFERENCE_NUMBER> {0:M} p.63, 64
        +2 TYPE <USER_REFERENCE_TYPE> {0:1} p.64
        +1 RIN <AUTOMATED_RECORD_ID> {0:1} p.43
        +1 <<CHANGE_DATE>> {0:1} p.31
        +1 <<NOTE_STRUCTURE>> {0:M} p.37
        +1 <<SOURCE_CITATION>> {0:M} p.39
        +1 <<MULTIMEDIA_LINK>> {0:M} p.37, 26
     */

    val (xref, _) = (node.xref, node.name) match {
      case (Some(xref), name) if name == "INDI" => (xref, name)
      case (None, name) if name != "INDI"       =>
        throw new RuntimeException(
          s"line ${node.lineNumber}: `${node.line}` tag name is invalid INDI is expected and xref is missing"
        )
      case (None, _) => throw new RuntimeException(s"line ${node.lineNumber}: `${node.line}` xref is missing")
      case (_, _)    =>
        throw new RuntimeException(s"line ${node.lineNumber}: `${node.line}` tag name is invalid INDI is expected")
    }

    val nameStructure: Ior[Seq[String], GedComPersonalNameStructure] =
      node.children.find(_.name == "NAME").fold(Ior.Left(Seq(s"Line ${node.lineNumber}: no NAME tag in block"))) {
        node =>
          readPersonalNameStructure(node)
      }

    val resnIor: Ior[Seq[String], Map[String, Option[ResnType]]] =
      node.children
        .find(_.name == "RESN")
        .fold(Ior.Right(Map.empty)) { node =>
          gedcomCommonParser.readTagContent(node).map { mapTags =>
            mapTags.map {
              case (key: String, resn: String) =>
                key -> ResnType.fromString(resn)
            }
          }
        }

    val sexIor: Ior[Seq[String], Map[String, String]] =
      node.children.find(_.name == "SEX").fold(Ior.Right(Map.empty)) { node =>
        gedcomCommonParser.readTagContent(node)
      }

    val eventsIor: Ior[Seq[String], List[GedcomEventBlock]] = node.children
      .filter { child =>
        Constants.individualsEvents.contains(child.name)
      }
      .foldLeft(Ior.Right(List.empty): Ior[Seq[String], List[GedcomEventBlock]]) {
        case (result, node) =>
          result.combine(gedcomEventParser.readEventBlock(node).map(List(_)))
      }

    val famsLinksIor: Ior[Seq[String], Set[Int]] = node.children
      .filter(_.name == "FAMS")
      .map { node =>
        node.xref.fold(
          Ior.left(Seq(s"line ${node.lineNumber}: `${node.line}` FAMS in INDI is invalid xref is expected"))
        )(xref => Ior.right(gedcomHashIdTable.getFamilyIdFromString(jobId, xref)))
      }
      .sequence
      .map(_.toSet)

    val famcLinksIor: Ior[Seq[String], Set[Int]] = node.children
      .filter(_.name == "FAMC")
      .map { node =>
        node.xref.fold(
          Ior.left(Seq(s"line ${node.lineNumber}: `${node.line}` FAMC in INDI is invalid xref is expected"))
        )(xref => Ior.right(gedcomHashIdTable.getFamilyIdFromString(jobId, xref)))
      }
      .sequence
      .map(_.toSet)

    val allTagList =
      List("RESN", "SEX", "NAME", "FAMS", "FAMC") ++ eventsIor.right.fold(List.empty[String])(_.map(_.tag))

    val ignoredContent: Ior[Seq[String], Map[String, String]] = node.children
      .filterNot(child => allTagList.contains(child.name))
      .map { node =>
        Ior.Left(Seq(s"Line ${node.lineNumber}: `${node.line}` in individual is not supported"))
      }
      .foldLeft(Ior.Right(Map.empty): Ior[Seq[String], Map[String, String]]) {
        case (result, element) =>
          result.combine(element)
      }

    for {
      name      <- nameStructure
      resn      <- resnIor
      sex       <- sexIor
      events    <- eventsIor
      famcLinks <- famcLinksIor
      famsLinks <- famsLinksIor
      _         <- ignoredContent
    } yield {
      GedcomIndiBlock(
        name,
        resn.getOrElse("RESN", None),
        sex.getOrElse("SEX", ""),
        gedcomHashIdTable.getIndividualIdFromString(jobId, xref),
        events,
        famcLinks,
        famsLinks
      )
    }

  }

  def readPersonalNameStructure(
      node: GedcomNode
  ): Ior[Seq[String], GedComPersonalNameStructure] = {
    /*
    1 NAME Firstname /Surname/ //only one supported
    2 TYPE <NAME_TYPE> // Not supported
    2 NPFX <NAME_PIECE_PREFIX>
    2 GIVN <NAME_PIECE_GIVEN>
    2 NICK <NAME_PIECE_NICKNAME>
    2 SPFX <NAME_PIECE_SURNAME_PREFIX
    2 NSFX <NAME_PIECE_SUFFIX>
    2 SURN <NAME_PIECE_SURNAME>
    2 FONE <NAME_PHONETIC_VARIATION> // Not supported
    2 ROMN <NAME_ROMANIZED_VARIATION> // Not supported
     */

    val tagList = List("NPFX", "GIVN", "NICK", "SPFX", "NSFX", "SURN")

    if (node.name != "NAME") {
      Ior.Left(List(s"Node `$node` is not a name structure"))
    } else {
      val nameIor: Ior[List[String], Map[String, String]] = gedcomCommonParser.readTagContent(node)

      val tagsContent: Seq[Ior[List[String], Map[String, String]]] = tagList.map { tag =>
        node.children.find(_.name == tag).fold(Ior.Right(Map.empty: Map[String, String])) { node =>
          gedcomCommonParser.readTagContent(node)
        }
      }

      val ignoredContent: Seq[Ior[Seq[String], Map[String, String]]] = node.children
        .filterNot(child => tagList.contains(child.name))
        .map { node =>
          Ior.Left(Seq(s"Line ${node.lineNumber}: `${node.line}` in name is not supported"))
        }

      val mergedContent: Ior[Seq[String], Map[String, String]] =
        (List(nameIor) ++ tagsContent ++ ignoredContent)
          .foldLeft(Ior.Right(Map.empty): Ior[Seq[String], Map[String, String]]) {
            case (result, element) =>
              result.combine(element)
          }

      mergedContent.map { contents =>
        GedComPersonalNameStructure(
          contents.getOrElse("NAME", ""),
          contents.getOrElse("NPFX", ""),
          contents.getOrElse("GIVN", ""),
          contents.getOrElse("NICK", ""),
          contents.getOrElse("SPFX", ""),
          contents.getOrElse("NSFX", ""),
          "surn"
        )
      }
    }
  }

  def gedcomIndiBlock2Sql(nodes: Seq[GedcomIndiBlock], base: Int): BatchSql = {
    val sqlStatement =
      s"""INSERT INTO genea_individuals (indi_id, base, indi_nom, indi_prenom, indi_sexe, indi_npfx, indi_givn, indi_nick, indi_spfx, indi_nsfx, indi_resn) VALUES
         | ({indi_id} + @startIndi, {base}, {surname}, {firstname}, {indi_sex}, {indi_npfx}, {indi_givn}, {indi_nick}, {indi_spfx}, {indi_nsfx}, {indi_resn})""".stripMargin

    val parameters: Seq[Seq[NamedParameter]] = nodes.map { node =>
      val nameRegex            = "([^/]*)/([^/]*)/(.*)".r
      val (firstname, surname) = node.nameStructure.name match {
        case nameRegex(firstname, surname, other) => (firstname, surname + " " + other)
        case _                                    => ("", "")
      }

      Seq[NamedParameter](
        "indi_id"   -> node.id,
        "base"      -> base,
        "surname"   -> surname,
        "firstname" -> firstname,
        "indi_sex"  -> node.sex,
        "indi_npfx" -> node.nameStructure.npfx,
        "indi_givn" -> node.nameStructure.givn,
        "indi_nick" -> node.nameStructure.nick,
        "indi_spfx" -> node.nameStructure.spfx,
        "indi_nsfx" -> node.nameStructure.nsfx,
        "indi_resn" -> node.resn.map(resn => s"$resn")
      )
    }

    BatchSql(sqlStatement, parameters.head, parameters.tail*)
  }
}
