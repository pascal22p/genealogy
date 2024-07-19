import com.typesafe.sbt.packager.docker.{DockerChmodType, DockerVersion}

ThisBuild / version := "0.1.0"
ThisBuild / organization := "parois.net"
ThisBuild / scalaVersion := "3.4.2"
ThisBuild / scalafmtOnCompile := true

Universal / javaOptions ++= Seq(
  "-Dpidfile.path=/dev/null"
)

Docker / packageName := "genealogie"
Docker / dockerVersion := Some(DockerVersion(0,1,0, None))
Docker / dockerBaseImage := "eclipse-temurin"
Docker / dockerExposedPorts ++= Seq(9123)
Docker / dockerChmodType := DockerChmodType.UserGroupWriteExecute
Docker / dockerUsername := Some("pascal22p")


lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := ".*Reverse.*",
    ScoverageKeys.coverageExcludedFiles := ".*Routes.*",
    ScoverageKeys.coverageMinimumStmtTotal := 89.5,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}


lazy val genealogy = (project in file("."))
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
