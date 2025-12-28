package views

import java.nio.file.Files
import java.nio.file.Paths

import scala.jdk.CollectionConverters.*
import scala.util.matching.Regex

import org.scalatestplus.play.*
import play.api.i18n.Lang
import play.api.i18n.Langs
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Application

class MessageKeysSpec extends PlaySpec {

  private val projectRoot = Paths.get(".")
  private val sourceDirs  = Seq("app", "conf")

  // Regex patterns for key lookups
  private val patterns: Seq[Regex] = Seq(
    """[Mm]essages\s*\(\s*"([^"]+)"""".r,    // Messages("key")
    """messagesApi\s*\(\s*"([^"]+)"""".r,    // messagesApi("key")
    """@\s*[Mm]essages\s*\(\s*"([^"]+)"""".r // @Messages("key") in Twirl
  )

  private def allFilesWithExtension(ext: String): Seq[java.nio.file.Path] = {
    sourceDirs.flatMap { dir =>
      val path = projectRoot.resolve(dir)
      if (Files.exists(path))
        Files
          .walk(path)
          .iterator()
          .asScala
          .filter(p => p.toString.endsWith(ext))
          .toSeq
      else Seq.empty
    }
  }

  private def extractKeysFromSource(): Set[String] = {
    val scalaFiles = allFilesWithExtension(".scala") ++ allFilesWithExtension(".scala.html")
    scalaFiles.flatMap { file =>
      val content = Files.readString(file)
      patterns.flatMap(_.findAllMatchIn(content).map(_.group(1)))
    }.toSet
  }

  private def extractKeysFromMessagesFile(lang: String = "en"): Set[String] = {
    val path = projectRoot.resolve(s"conf/messages.$lang")
    if (Files.exists(path)) {
      val lines = Files.readAllLines(path).asScala
      lines
        .filterNot(_.trim.startsWith("#"))
        .flatMap(_.split("=").headOption)
        .map(_.trim)
        .filter(_.nonEmpty)
        .toSet
    } else Set.empty
  }

  // Spin up a Play app with i18n enabled
  val app: Application                  = new GuiceApplicationBuilder().build()
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val langs: Langs                      = app.injector.instanceOf[Langs]

  "Messages" should {

    "not have missing or extra keys in any language" in {
      val allLangs: Seq[Lang] = langs.availables
      val defaultMessages     = messagesApi.messages(Lang("fr").code)

      allLangs.filterNot(lang => lang.code == "fr").foreach { lang =>
        val langMessages = messagesApi.messages(lang.code)

        // Keys missing in this language compared to default
        val missing = defaultMessages.keySet.diff(langMessages.keySet)

        // Extra keys in this language that are not in default
        val extra = langMessages.keySet.diff(defaultMessages.keySet)

        withClue(s"Missing messages for ${lang.code}: ${missing.mkString(", ")}") {
          missing mustBe empty
        }

        withClue(s"Extra messages for ${lang.code}: ${extra.mkString(", ")}") {
          extra mustBe empty
        }
      }
    }
  }

  "Message keys" should {
    "exist in conf/messages" in {
      val usedKeys    = extractKeysFromSource()
      val definedKeys = extractKeysFromMessagesFile()

      val missing = usedKeys.diff(definedKeys)

      withClue(s"Missing message definitions: ${missing.mkString(", ")}") {
        missing mustBe empty
      }
    }
  }
}
