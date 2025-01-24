package config

import javax.inject.Inject
import javax.inject.Singleton

import play.api.Configuration

@Singleton
class AppConfig @Inject() (configuration: Configuration) {

  val mediaPath: String = configuration.get[String]("media-path")

  val databaseName: String = configuration.get[String]("database.name")
}
