import com.typesafe.sbt.packager.docker.DockerChmodType


ThisBuild / version := "0.1.0"
ThisBuild / organization := "parois.net"
ThisBuild / scalaVersion := "3.4.2"
ThisBuild / scalafmtOnCompile := true

Universal / javaOptions ++= Seq(
  "-Dpidfile.path=/dev/null"
)



packageName := "genealogy"
dockerBaseImage := "eclipse-temurin:21"
dockerExposedPorts ++= Seq(9123)
dockerChmodType := DockerChmodType.UserGroupWriteExecute
dockerUsername := Some("pascal22p")


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

