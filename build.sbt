import com.typesafe.sbt.packager.docker.DockerChmodType
import wartremover.Wart.{DefaultArguments, Equals, ImplicitParameter, Overloading, Recursion, Any}
import play.twirl.sbt.Import.TwirlKeys

import scala.sys.process.Process


ThisBuild / version := "latest"
ThisBuild / organization := "parois.net"
ThisBuild / scalaVersion := "3.6.2"
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
    dockerBuildxSettings,
    Compile / compile / wartremoverErrors ++= Warts.allBut(DefaultArguments, ImplicitParameter, Overloading, Equals, Recursion, Any),
    wartremoverExcluded ++= (Compile / routes).value,
    wartremoverExcluded += (TwirlKeys.compileTemplates / target).value,
    resolvers ++= Seq(
      Resolver.jcenterRepo
    ),
    routesImport ++= Seq("models.SourCitationType.SourCitationType", "models.SourCitationType"),
    resolvers ++=Seq(
      MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2"), 
      Resolver.url("HMRC-open-artefacts-ivy2", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns),
    ),
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.govukfrontend.views.html.components._"
    ),
    semanticdbEnabled := true,
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:noAutoTupling",
      "-language:strictEquality",
      "-Xkind-projector",
      "-Wvalue-discard",
      "-Wunused:all",
      //"-Xfatal-warnings",
      //"-Yexplicit-nulls",
      "-Wsafe-init",
      "-explain",
      "-Wconf:msg=unused import&src=html/.*:s",
      "-Wconf:src=routes/.*:s"
    )
  )

  Test / scalacOptions --= Seq("-language:strictEquality")

  