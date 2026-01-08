package services.gedcom

import javax.inject.Inject

import anorm.BatchSql
import anorm.NamedParameter
import cats.data.Ior
import com.google.inject.Singleton
import models.gedcom.GedcomEventBlock
import models.gedcom.GedcomFamilyBlock
import models.gedcom.GedcomNode
import models.ResnType
import utils.Constants

@Singleton
class GedcomFamilyParser @Inject() (
    gedcomCommonParser: GedcomCommonParser,
    gedcomHashIdTable: GedcomHashIdTable,
    gedcomEventParser: GedcomEventParser
) {

  def readFamilyBlock(node: GedcomNode, jobId: String): Ior[Seq[String], GedcomFamilyBlock] = {
    /*
    n @<XREF:FAM>@ FAM
      +1 RESN <RESTRICTION_NOTICE>
      +1 <<FAMILY_EVENT_STRUCTURE>>
      +1 HUSB @<XREF:INDI>@
      +1 WIFE @<XREF:INDI>@
      +1 CHIL @<XREF:INDI>@
      +1 NCHI <COUNT_OF_CHILDREN>
      +1 SUBM @<XREF:SUBM>@
      +1 <<LDS_SPOUSE_SEALING>>
      +1 REFN <USER_REFERENCE_NUMBER>
        +2 TYPE <USER_REFERENCE_TYPE>
      +1 RIN <AUTOMATED_RECORD_ID>
      +1 <<CHANGE_DATE>>
      +1 <<NOTE_STRUCTURE>>
      +1 <<SOURCE_CITATION>>
      +1 <<MULTIMEDIA_LINK>>
     */

    val (xref, _) = (node.xref, node.name) match {
      case (Some(xref), name) if name == "FAM" => (xref, name)
      case (None, name) if name != "FAM"       =>
        throw new RuntimeException(
          s"line ${node.lineNumber}: `${node.line}` tag name is invalid FAM is expected and xref is missing"
        )
      case (None, _) => throw new RuntimeException(s"line ${node.lineNumber}: `${node.line}` xref is missing")
      case (_, _)    =>
        throw new RuntimeException(s"line ${node.lineNumber}: `${node.line}` tag name is invalid FAM is expected")
    }

    val resnIor: Ior[Seq[String], Option[ResnType.ResnType]] =
      node.children
        .find(_.name == "RESN")
        .fold(Ior.right(None): Ior[Seq[String], Option[ResnType.ResnType]]) { node =>
          gedcomCommonParser.readTagContent(node).map(resn => resn.get("RESN").flatMap(ResnType.fromString))
        }

    val husbIor: Ior[Seq[String], Option[String]] =
      node.children.find(_.name == "HUSB").fold(Ior.Right(None): Ior[Seq[String], Option[String]]) { node =>
        node.xref.fold(
          Ior.left(Seq(s"line ${node.lineNumber}: `${node.line}` HUSB is invalid xref is expected"))
        )(node => Ior.Right(Some(node)))
      }
    val wifeIor: Ior[List[String], Option[String]] =
      node.children.find(_.name == "WIFE").fold(Ior.Right(None): Ior[List[String], Option[String]]) { node =>
        node.xref.fold(
          Ior.left(List(s"line ${node.lineNumber}: `${node.line}` WIFE is invalid xref is expected"))
        )(node => Ior.Right(Some(node)))
      }
    val childrenIor: Ior[List[String], List[String]] =
      node.children.filter(_.name == "CHIL").foldLeft(Ior.Right(List.empty): Ior[List[String], List[String]]) {
        case (children, node) =>
          val child: Ior[List[String], List[String]] = node.xref.fold(
            Ior.left(List(s"line ${node.lineNumber}: `${node.line}` HUSB is invalid xref is expected"))
          )(child => Ior.Right(List(child)))
          children.combine(child)
      }

    val eventsIor: Ior[Seq[String], Seq[GedcomEventBlock]] = node.children
      .filter { child =>
        Constants.familyEvents.contains(child.name)
      }
      .foldLeft(Ior.Right(Seq.empty): Ior[Seq[String], Seq[GedcomEventBlock]]) {
        case (result, node) =>
          result.combine(gedcomEventParser.readEventBlock(node).map(Seq(_)))
      }

    val allTagList = List("RESN", "HUSB", "WIFE", "CHIL") ++ eventsIor.right.fold(List.empty[String])(_.map(_.tag))

    val ignoredContent: Ior[Seq[String], Map[String, String]] = Ior.Left(
      node.children
        .filterNot(child => allTagList.contains(child.name))
        .foldLeft(List.empty[String]) {
          case (result, node) =>
            result ++ List(s"Line ${node.lineNumber}: `${node.line}` in family is not supported")
        }
    )

    for {
      resn     <- resnIor
      husb     <- husbIor
      wife     <- wifeIor
      children <- childrenIor
      events   <- eventsIor
      _        <- ignoredContent
    } yield {
      GedcomFamilyBlock(
        gedcomHashIdTable.getFamilyIdFromString(jobId, xref),
        wife.map(gedcomHashIdTable.getIndividualIdFromString(jobId, _)),
        husb.map(gedcomHashIdTable.getIndividualIdFromString(jobId, _)),
        children.toSet.map(gedcomHashIdTable.getIndividualIdFromString(jobId, _)),
        events,
        resn
      )
    }

  }

  def gedcomFamilyBlock2Sql(nodes: Seq[GedcomFamilyBlock], base: Int): BatchSql = {
    val sqlStatement =
      s"""INSERT INTO `genea_familles` (`familles_id`, `base`, `familles_wife`, `familles_husb`, `familles_resn`, `familles_refn`, `familles_refn_type`)
         |VALUES ({familles_id} + @startFamily, {base}, {familles_wife} + @startIndi, {familles_husb} + @startIndi, {familles_resn}, {familles_refn}, {familles_refn_type})""".stripMargin

    val parameters = nodes.map { node =>
      Seq[NamedParameter](
        "familles_id"        -> node.id,
        "base"               -> base,
        "familles_wife"      -> node.wife,
        "familles_husb"      -> node.husb,
        "familles_resn"      -> node.resn.map(resn => s"$resn"),
        "familles_refn"      -> "",
        "familles_refn_type" -> ""
      )
    }

    BatchSql(sqlStatement, parameters.head, parameters.tail*)
  }

  def gedcomChildren2Sql(nodes: Seq[GedcomFamilyBlock]): BatchSql = {
    val sqlStatement =
      s"""INSERT INTO `rel_familles_indi` (`familles_id`, `indi_id`, `rela_type`, `rela_stat`)
         |VALUES ({familles_id} + @startFamily, {indi_id} + @startIndi, {rela_type}, {rela_stat})""".stripMargin

    val parameters = nodes.flatMap { node =>
      node.children.toList.map { child =>
        Seq[NamedParameter](
          "familles_id" -> node.id,
          "indi_id"     -> child,
          "rela_type"   -> "",
          "rela_stat"   -> Option.empty[String]
        )
      }
    }

    BatchSql(sqlStatement, parameters.head, parameters.tail*)
  }
}
