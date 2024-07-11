import com.typesafe.sbt.packager.docker.DockerChmodType

ThisBuild / version := "0.1.0"
ThisBuild / organization := "parois.net"
ThisBuild / scalaVersion := "3.4.2"
ThisBuild / scalafmtOnCompile := true

Universal / javaOptions ++= Seq(
  "-Dpidfile.path=/dev/null"
)

Docker / packageName := "genealogie"
Docker / version := version.value

dockerBaseImage := "eclipse-temurin"
dockerExposedPorts ++= Seq(9123)
dockerChmodType := DockerChmodType.UserGroupWriteExecute

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;" + "config/ErrorHandler*;" + ".*routePages.*;" +
      "config/FandfSessionCache*;" + "models/.data/..*;view.*;models.*;" + ".*BuildInfo.*;" + ".*Routes.*;" +
      ".*FandFTestOnlyConnector*;" + ".*AddPerformanceTestDataController*;" + ".*DropCollectionController*;",
    ScoverageKeys.coverageMinimumStmtTotal := 89.5,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}


lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings(
    PlayKeys.playDefaultPort := 9123,
    libraryDependencies ++= LibDependencies.all,
    scoverageSettings
  )


// Adds additional packages into Twirl

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "parois.net.binders._"
