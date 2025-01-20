package services.gedcom

import javax.inject.Inject

import cats.data.Ior
import com.google.inject.Singleton
import models.gedcom.GedComPersonalNameStructure
import models.gedcom.GedcomIndiBlock
import models.gedcom.GedcomNode
import models.ResnType
import models.ResnType.ResnType

@Singleton
class GedcomIndividualParser @Inject() (gedcomCommonParser: GedcomCommonParser, gedcomHashIdTable: GedcomHashIdTable) {

  def readIndiBlock(node: GedcomNode): Ior[List[String], GedcomIndiBlock] = {
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
    val allTagList = List("RESN", "SEX", "NAME")

    val (xref, name) = (node.xref, node.name) match {
      case (Some(xref), name) if name == "INDI" => (xref, name)
      case (None, name) if name != "INDI" =>
        throw new RuntimeException(
          s"line ${node.lineNumber}: `${node.line}` tag name is invalid INDI is expected and xref is missing"
        )
      case (None, _) => throw new RuntimeException(s"line ${node.lineNumber}: `${node.line}` xref is missing")
      case (_, name) =>
        throw new RuntimeException(s"line ${node.lineNumber}: `${node.line}` tag name is invalid INDI is expected")
    }

    val nameStructure: Ior[List[String], GedComPersonalNameStructure] =
      node.children.find(_.name == "NAME").fold(Ior.Left(List(s"Line ${node.lineNumber}: no NAME tag in block"))) {
        node =>
          readPersonalNameStructure(node)
      }

    val resnIor: Ior[List[String], Map[String, Option[ResnType]]] =
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

    val sexIor: Ior[List[String], Map[String, String]] =
      node.children.find(_.name == "SEX").fold(Ior.Right(Map.empty)) { node =>
        gedcomCommonParser.readTagContent(node)
      }

    val ignoredContent: Ior[List[String], Map[String, String]] = node.children
      .filterNot(child => allTagList.contains(child.name))
      .map { node =>
        Ior.Left(List(s"Line ${node.lineNumber}: `${node.line}` is not supported"))
      }
      .foldLeft(Ior.Right(Map.empty): Ior[List[String], Map[String, String]]) {
        case (result, element) =>
          result.combine(element)
      }

    for {
      name    <- nameStructure
      resn    <- resnIor
      sex     <- sexIor
      ignored <- ignoredContent
    } yield {
      GedcomIndiBlock(
        name,
        resn.getOrElse("RESN", None),
        sex.getOrElse("SEX", ""),
        gedcomHashIdTable.getIndividualIdFromString(xref)
      )
    }

  }

  def readPersonalNameStructure(
      node: GedcomNode
  ): Ior[List[String], GedComPersonalNameStructure] = {
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

      val tagsContent: List[Ior[List[String], Map[String, String]]] = tagList.map { tag =>
        node.children.find(_.name == tag).fold(Ior.Right(Map.empty: Map[String, String])) { node =>
          gedcomCommonParser.readTagContent(node)
        }
      }

      val ignoredContent: List[Ior[List[String], Map[String, String]]] = node.children
        .filterNot(child => tagList.contains(child.name))
        .map { node =>
          Ior.Left(List(s"Line ${node.lineNumber}: `${node.line}` is not supported"))
        }

      val mergedContent: Ior[List[String], Map[String, String]] =
        (List(nameIor) ++ tagsContent ++ ignoredContent)
          .foldLeft(Ior.Right(Map.empty): Ior[List[String], Map[String, String]]) {
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
}
