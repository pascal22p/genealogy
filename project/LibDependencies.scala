import play.sbt.PlayImport.{guice, jdbc}
import sbt.*

object LibDependencies {
  val libraryDependenciesCompile: Seq[ModuleID] = Seq(
    guice,
    jdbc,
    "org.mariadb.jdbc"        %  "mariadb-java-client" % "3.5.1",
    "org.playframework.anorm" %% "anorm"               % "2.7.0",
    "org.typelevel"           %% "cats-core"           % "2.12.0",
    "org.mindrot"             %  "jbcrypt"             % "0.4"
  )

  val libraryDependenciesTest: Seq[ModuleID] = Seq(
    "org.scalatestplus.play"  %% "scalatestplus-play" % "7.0.1" % Test,
    "org.scalatestplus"       %%  "mockito-5-10"      % "3.2.18.0" % Test,
  )

  val all: Seq[ModuleID]  = libraryDependenciesCompile ++ libraryDependenciesTest
}