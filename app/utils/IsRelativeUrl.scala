package utils

import java.net.URI

import scala.util.Try

extension (url: String) {
  def isRelativeUrl: Boolean = {
    val trimmed = url.trim
    Try(new URI(trimmed)).toOption.exists { uri =>
      uri.getScheme == null &&
      uri.getRawAuthority == null &&
      trimmed.matches("""^[/][^/\\].*""") &&
      !trimmed.contains('\r') &&
      !trimmed.contains('\n')
    }
  }
}
