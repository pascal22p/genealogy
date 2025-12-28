package utils

import java.io.BufferedInputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object FileUtils {

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

    charset.getOrElse(StandardCharsets.UTF_8)
  }

  def ReadGedcomAsString(filePath: String): String = {
    val charset = detectGedcomCharset(Paths.get(filePath))
    Files.readString(Paths.get(filePath), charset)
  }
}
