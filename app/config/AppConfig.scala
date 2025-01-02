package config

import javax.inject.Singleton
import javax.inject.Inject
import play.api.Configuration

@Singleton
class AppConfig @Inject()(configuration: Configuration) {

    val mediaPath = configuration.get[String](s"media-path")
}
