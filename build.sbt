import com.typesafe.sbt.packager.docker.DockerChmodType

import scala.sys.process.Process


ThisBuild / version := "latest"
ThisBuild / organization := "parois.net"
ThisBuild / scalaVersion := "3.4.2"
ThisBuild / scalafmtOnCompile := true

Test / parallelExecution := true
Test / Keys.fork := true

Universal / javaOptions ++= Seq(
  "-Dpidfile.path=/dev/null"
)

packageName := "genealogy"
dockerBaseImage := "eclipse-temurin:21"
dockerExposedPorts ++= Seq(9123)
dockerChmodType := DockerChmodType.UserGroupWriteExecute
dockerUsername := Some("pascal22p")

lazy val ensureDockerBuildx = taskKey[Unit]("Ensure that docker buildx configuration exists")
lazy val dockerBuildWithBuildx = taskKey[Unit]("Build docker images using buildx")
lazy val dockerBuildxSettings = Seq(
  ensureDockerBuildx := {
    if (Process("docker buildx inspect multi-arch-builder").! == 1) {
      Process("docker buildx create --use --name multi-arch-builder", baseDirectory.value).!
    }
  },
  dockerBuildWithBuildx := {
    streams.value.log("Building and pushing image with Buildx")
    dockerAliases.value.foreach(
      alias => Process("docker buildx build --platform=linux/arm64,linux/amd64 --push -t " +
        alias + " .", baseDirectory.value / "target" / "docker"/ "stage").!
    )
  },
  Docker / publish := Def.sequential(
  Docker / publishLocal,
    ensureDockerBuildx,
    dockerBuildWithBuildx
  ).value
)


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
    scoverageSettings,
    dockerBuildxSettings
  )

