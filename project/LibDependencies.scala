import play.sbt.PlayImport.{guice, jdbc}
import sbt.*

object LibDependencies {
  val libraryDependenciesCompile: Seq[ModuleID] = Seq(
    guice,
    jdbc,
    "org.mariadb.jdbc"        % "mariadb-java-client" % "3.4.0",
    "org.playframework.anorm" %% "anorm"              % "2.7.0",
    "org.typelevel"           %% "cats-core"          % "2.12.0"
  )

  val libraryDependenciesTest: Seq[ModuleID] = Seq(
    "org.scalatestplus.play"  %% "scalatestplus-play" % "7.0.1" % Test
  )

  val all: Seq[ModuleID]  = libraryDependenciesCompile ++ libraryDependenciesTest
}