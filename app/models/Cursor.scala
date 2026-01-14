package models

final case class Cursor(name: String, index: Int, birthJd: Int, deathJd: Int, label: String) {
  def anchor: String = java.net.URLEncoder.encode(s"$name#$index#$birthJd#$deathJd", "UTF-8")
}
