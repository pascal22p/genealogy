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
  // private val lineRegexp: Regex             = "^([0-9]+)\\s+.*".r
  private val tagAndXrefRegexp: Regex       = "^\\s*[0-9]+\\s+([_A-Za-z]+)\\s*(@([^@]+)@|)\\s*(.+|)".r
  private val level0TagAndXrefRegexp: Regex = "^\\s*0\\s+(@([^@]+)@|)\\s*([A-Za-z]+).*".r

  @SuppressWarnings(Array("org.wartremover.warts.While"))
  def getBlocks(gedcomString: Iterator[String], level: Int = 0): Iterator[(Int, Vector[String])] = {
    Iterator.unfold(gedcomString.zipWithIndex.buffered) { it =>
      while (it.hasNext && it.head._1.trim.isEmpty) {
        it.next()
        ()
      }

      if (!it.hasNext) {
        None
      } else {
        val (line, lineNo) = it.next()

        if (!line.trim.startsWith(s"$level"))
          throw new RuntimeException(
            s"Expected block start level $level at line $lineNo, found: $line"
          )

        val b = Vector.newBuilder[String]
        b += line.trim

        while (
          it.hasNext &&
          it.head._1.trim.nonEmpty &&
          !it.head._1.trim.startsWith(s"$level")
        ) {
          b += it.next()._1.trim
        }

        Some(((lineNo, b.result()), it))
      }
    }
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
    GedcomObject(getListNodes(gedcomIterator).toSeq)
  }

  private[gedcom] final def getListNodes(
      gedcomString: Iterator[String],
      level: Int = 0,
      rootLineNumber: Int = 0
  ): Iterator[GedcomNode] = {
    getBlocks(gedcomString, level).map {
      case (lineNumber, block) =>
        val head                     = block.head
        val (xref, tagName, content) = head match {
          case level0TagAndXrefRegexp(_, xref, tagName)    => (Option(xref), tagName, None)
          case tagAndXrefRegexp(tagName, _, xref, content) =>
            val newOption = Option(content) match {
              case Some(s) if s.isEmpty => None
              case o                    => o
            }
            (Option(xref), tagName, newOption)
          case _ => throw new RuntimeException(s"Could not find tag in `$head`")
        }
        if (block.tail.nonEmpty) {
          val subTree: Seq[GedcomNode] = getListNodes(block.tail.iterator, level + 1, lineNumber + 1).toSeq
          GedcomNode(
            line = head,
            lineNumber = lineNumber + rootLineNumber,
            name = tagName,
            level = level,
            xref = xref,
            content = content,
            children = subTree
          )
        } else {
          GedcomNode(
            line = head,
            lineNumber = lineNumber + rootLineNumber,
            name = tagName,
            level = level,
            xref = xref,
            content = content,
            children = Seq.empty[GedcomNode]
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
