package config

import javax.inject.Inject
import javax.inject.Singleton

import play.api.Configuration

@Singleton
class AppConfig @Inject() (configuration: Configuration) {
  val protocol: String    = configuration.get[String]("protocol")
  val allowedHost: String = configuration.get[String]("allowedHost")
  val appName: String     = configuration.get[String]("appName")

  val mediaPath: String          = configuration.get[String]("media-path").reverse.dropWhile(_ == '/').reverse
  val uploadPath: String         = configuration.get[String]("upload-path").reverse.dropWhile(_ == '/').reverse
  val externalAssetsPath: String = configuration.get[String]("external-assets-path").reverse.dropWhile(_ == '/').reverse

  val databaseName: String = configuration.get[String]("database.name")

  val redactedMask: String = "*******"

  val commitHash: String = sys.props.getOrElse("git.commit.hash", "unknown")

  val pageSize = 20

  val customJsFile: Option[String] =
    Option(configuration.get[String]("custom.js.file")).map(_.trim).filter(_.nonEmpty)
}
