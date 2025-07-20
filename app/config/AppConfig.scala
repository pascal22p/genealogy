package config

import javax.inject.Inject
import javax.inject.Singleton

import play.api.Configuration

@Singleton
class AppConfig @Inject() (configuration: Configuration) {
  val protocol: String    = configuration.get[String]("protocol")
  val allowedHost: String = configuration.get[String]("allowedHost")

  val mediaPath: String          = configuration.get[String]("media-path")
  val externalAssetsPath: String = configuration.get[String]("external-assets-path")

  val databaseName: String = configuration.get[String]("database.name")

  val redactedMask: String = "*******"

  val commitHash: String = sys.props.getOrElse("git.commit.hash", "unknown")
}
