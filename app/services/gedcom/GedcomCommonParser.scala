package services.gedcom

import javax.inject.Inject
import javax.inject.Singleton

import scala.util.matching.Regex

import cats.data.Ior
import models.gedcom.GedcomNode
import models.gedcom.GedcomObject
import utils.FileUtils

@Singleton
class GedcomCommonParser @Inject() () {
  private val lineRegexp: Regex             = "^([0-9]+)\\s+.*".r
  private val tagAndXrefRegexp: Regex       = "^\\s*[0-9]+\\s+([_A-Za-z]+)\\s*(@([^@]+)@|)\\s*(.+|)".r
  private val level0TagAndXrefRegexp: Regex = "^\\s*0\\s+(@([^@]+)@|)\\s*([A-Za-z]+).*".r

  def getBlocks(gedcomString: Iterator[String], level: Int = 0): List[(Int, String)] = {
    gedcomString.zipWithIndex
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

  def getSamplePlaces(
      gedcomPath: String,
      sampleSize: Int,
      separatorOption: Option[String] = None,
      paddingOrderOption: Option[String] = None
  ): Set[List[String]] = {
    val placePattern              = "[ ]*[0-9]+[ ]*PLAC[ ]*(.*)".r
    val gedcomIterator            = FileUtils.readGedcomAsIterator(gedcomPath)
    val uniquePlaces: Set[String] = gedcomIterator
      .collect {
        case placePattern(place) => place.trim
      }
      .take(sampleSize)
      .toSet

    (for {
      separator    <- separatorOption
      paddingOrder <- paddingOrderOption
    } yield {
      val splitLines = uniquePlaces.map(_.split(separator).map(_.trim).toList)
      val maxSize    = splitLines.map(_.size).max
      splitLines.map { parts =>
        val padSize = maxSize - parts.size
        if (paddingOrder == "left") {
          List.fill(padSize)("") ++ parts
        } else {
          parts ++ List.fill(padSize)("")
        }
      }
    }).getOrElse(uniquePlaces.map(List(_)))
  }

  final def getTree(gedcomPath: String): GedcomObject = {
    val gedcomIterator = FileUtils.readGedcomAsIterator(gedcomPath)
    GedcomObject(getListNodes(gedcomIterator))
  }

  private[gedcom] final def getListNodes(
      gedcomString: Iterator[String],
      level: Int = 0,
      rootLineNumber: Int = 0
  ): List[GedcomNode] = {
    val blocks = getBlocks(gedcomString, level)
    blocks.map {
      case (lineNumber, block) =>
        val subBlock                 = block.linesIterator
        val subBlockHeader: String   = subBlock.nextOption().getOrElse("")
        val subBlockBody             = subBlock
        val (xref, tagName, content) = subBlockHeader match {
          case level0TagAndXrefRegexp(_, xref, tagName)    => (Option(xref), tagName, None)
          case tagAndXrefRegexp(tagName, _, xref, content) =>
            val newOption = Option(content) match {
              case Some(s) if s.isEmpty => None
              case o                    => o
            }
            (Option(xref), tagName, newOption)
          case _ => throw new RuntimeException(s"Could not find tag in `$subBlockHeader`")
        }
        if (subBlockBody.nonEmpty) {
          val subTree: List[GedcomNode] = getListNodes(subBlockBody, level + 1, lineNumber + 1)
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
