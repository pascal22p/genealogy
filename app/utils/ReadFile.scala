package utils

import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject

import utils.gedcom.CharsetUtil

class ReadFile @Inject() (charsetUtil: CharsetUtil) {

  def asString(filePath: String): String = {
    val charset = charsetUtil.detectGedcomCharset(Paths.get(filePath))
    Files.readString(Paths.get(filePath), charset)
  }
}
