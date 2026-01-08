package utils

import java.io.BufferedInputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import scala.collection.BufferedIterator

object FileUtils {

  private val anselMap: Map[String, String] = Map(
    "âe" -> "é",
    "áe" -> "è",
    "ãe" -> "ê",
    "èe" -> "ë",
    "âo" -> "ó",
    "áo" -> "ò",
    "ão" -> "ô",
    "èo" -> "ö",
    "âa" -> "á",
    "áa" -> "à",
    "ãa" -> "â",
    "èa" -> "ä",
    "âu" -> "ú",
    "áu" -> "ù",
    "ãu" -> "û",
    "èu" -> "ü",
    "âi" -> "í",
    "ái" -> "ì",
    "ãi" -> "î",
    "èi" -> "ï",
    "ây" -> "ý",
    "èy" -> "ÿ",
    "ðc" -> "ç",
    "~n" -> "ñ",
    "âE" -> "É",
    "áE" -> "È",
    "ãE" -> "Ê",
    "èE" -> "Ë",
    "âO" -> "Ó",
    "áO" -> "Ò",
    "ãO" -> "Ô",
    "èO" -> "Ö",
    "âA" -> "Á",
    "áA" -> "À",
    "ãA" -> "Â",
    "èA" -> "Ä",
    "âU" -> "Ú",
    "áU" -> "Ù",
    "ãU" -> "Û",
    "èU" -> "Ü",
    "âI" -> "Í",
    "áI" -> "Ì",
    "ãI" -> "Î",
    "èI" -> "Ï",
    "âY" -> "Ý",
    "èY" -> "\u009F", // note: original had a control char
    "ðC" -> "Ç",
    "~N" -> "Ñ"
  )

  def detectGedcomCharset(path: Path): Charset = {
    val bufferSize = 4096
    val buffer     = new Array[Byte](bufferSize)

    val in        = new BufferedInputStream(Files.newInputStream(path))
    val bytesRead = in.read(buffer)
    in.close()
    val headerBytes = buffer.take(bytesRead)

    // Decode as ASCII-compatible to find CHAR line
    val header = new String(headerBytes, StandardCharsets.ISO_8859_1)

    val charset = header.linesIterator
      .find(_.contains(" CHAR "))
      .map(_.trim)
      .collect {
        case line if line.endsWith("UTF-8")  => StandardCharsets.UTF_8
        case line if line.endsWith("UTF-16") => StandardCharsets.UTF_16
        case line if line.endsWith("ASCII")  => StandardCharsets.US_ASCII
        case line if line.endsWith("ANSEL")  => StandardCharsets.ISO_8859_1 // closest practical match
      }

    charset.getOrElse(StandardCharsets.ISO_8859_1)
  }

  def readGedcomAsString(filePath: String): String = {
    val charset = detectGedcomCharset(Paths.get(filePath))
    val s       = Files.readString(Paths.get(filePath), charset)
    charset.name match {
      case "ISO-8859-1" =>
        anselMap.foldLeft(s) {
          case (acc, (key, value)) =>
            acc.replace(key, value)
        }
      case _ => s
    }
  }

  def readGedcomAsIterator(filePath: String): BufferedIterator[String] & AutoCloseable = {
    val charset = detectGedcomCharset(Paths.get(filePath))
    val source  = scala.io.Source.fromFile(filePath, charset.name)

    val lineIterator = charset.name match {
      case "ISO-8859-1" =>
        source.getLines().map { line =>
          anselMap.foldLeft(line) {
            case (acc, (key, value)) =>
              acc.replace(key, value)
          }
        }
      case _ =>
        source.getLines()
    }

    new BufferedIterator[String] with AutoCloseable {
      private val underlying = lineIterator.buffered
      def hasNext: Boolean   = underlying.hasNext
      def next(): String     = underlying.next()
      def head: String       = underlying.head
      def close(): Unit      = source.close()
    }
  }

}
