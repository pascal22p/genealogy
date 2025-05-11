package services.gedcom

import javax.inject.Inject
import javax.inject.Singleton

import scala.util.matching.Regex

import cats.data.Ior
import models.gedcom.GedcomNode

@Singleton
class GedcomCommonParser @Inject() () {
  private val lineRegexp: Regex             = "^([0-9]+)\\s+.*".r
  private val tagAndXrefRegexp: Regex       = "^\\s*[0-9]+\\s+([A-Za-z]+)\\s*(@([^@]+)@|)\\s*(.+|)".r
  private val level0TagAndXrefRegexp: Regex = "^\\s*0\\s+(@([^@]+)@|)\\s*([A-Za-z]+).*".r

  def getBlocks(gedcomString: String, level: Int = 0): List[(Int, String)] = {
    scala.io.Source
      .fromString(gedcomString)
      .getLines
      .zipWithIndex
      .foldLeft(List.empty[(Int, String)]) {
        case (tree, (line, lineNumber)) =>
          line.trim match {

            case lineRegexp(id) if id.toInt < level =>
              throw new RuntimeException("found content at lower level")
            case lineRegexp(id) if id.toInt == level =>
              (lineNumber, line.trim) :: tree
            case lineRegexp(_) =>
              val (oldLineNumber, newLines) = tree.headOption.fold((0, "")) { (oldLineNumber, block) =>
                (oldLineNumber, block + "\n" + line.trim)
              }
              (oldLineNumber, newLines) :: tree.drop(1)
            case _ =>
              tree
          }
      }
      .reverse
      .filter(_._2.nonEmpty)
  }

  final def getTree(gedcomString: String, level: Int = 0, rootLineNumber: Int = 0): List[GedcomNode] = {
    val blocks = getBlocks(gedcomString, level)
    blocks.map {
      case (lineNumber, block) =>
        val subBlock               = scala.io.Source.fromString(block).getLines
        val subBlockHeader: String = subBlock.nextOption().getOrElse("")
        val subBlockBody           = subBlock.mkString("\n")
        val (xref, tagName, content) = subBlockHeader match {
          case level0TagAndXrefRegexp(_, xref, tagName) => (Option(xref), tagName, None)
          case tagAndXrefRegexp(tagName, _, xref, content) =>
            val newOption = Option(content) match {
              case Some(s) if s.isEmpty => None
              case o                    => o
            }
            (Option(xref), tagName, newOption)
          case _ => throw new RuntimeException(s"Could not find tag in `$subBlockHeader`")
        }
        if (subBlockBody.nonEmpty) {
          val subTree: List[GedcomNode] = getTree(subBlockBody, level + 1, lineNumber + 1)
          GedcomNode(
            line = subBlockHeader,
            lineNumber = lineNumber + rootLineNumber,
            name = tagName,
            level = level,
            xref = xref,
            content = content,
            children = subTree
          )
        } else {
          GedcomNode(
            line = subBlockHeader,
            lineNumber = lineNumber + rootLineNumber,
            name = tagName,
            level = level,
            xref = xref,
            content = content,
            children = List.empty[GedcomNode]
          )
        }
    }
  }

  def readTagContent(node: GedcomNode): Ior[List[String], Map[String, String]] = {
    node.content match {
      case None =>
        Ior.Both(
          List(s"Line ${node.lineNumber}: `${node.line}`. ${node.name} content is missing"),
          Map(node.name -> "")
        )
      case Some(content) => Ior.Right(Map(node.name -> content))
    }
  }

}
