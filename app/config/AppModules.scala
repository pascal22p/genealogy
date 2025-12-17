package config

import play.api.Configuration
import play.api.Environment
import repositories.*
import play.api.inject.Binding
import play.api.inject.Module

class AppModules extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[?]] =
    Seq(
      bind[JourneyCacheRepository].to[InMemoryJourneyCacheRepository]
    )
}
