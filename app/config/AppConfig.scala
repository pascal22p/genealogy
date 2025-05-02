package config

import javax.inject.Inject
import javax.inject.Singleton

import play.api.Configuration

@Singleton
class AppConfig @Inject() (configuration: Configuration) {

  val mediaPath: String = configuration.get[String]("media-path")

  val databaseName: String = configuration.get[String]("database.name")

  val redactedMask: String = "*******"

  val commitHash: String = sys.props.getOrElse("git.commit.hash", "unknown")
}
