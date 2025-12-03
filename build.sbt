import com.typesafe.sbt.packager.docker.{Cmd, DockerAlias, DockerChmodType, ExecCmd}
import wartremover.Wart.{Any, DefaultArguments, Equals, ImplicitParameter, IterableOps, MutableDataStructures, Nothing, Overloading, Recursion, SeqApply, Throw, Var}
import play.twirl.sbt.Import.TwirlKeys
import com.github.sbt.git.SbtGit.git

import scala.sys.process.Process

enablePlugins(DockerPlugin)
enablePlugins(GitPlugin)

ThisBuild / version := "0.3.0"
ThisBuild / organization := "parois.net"
ThisBuild / scalaVersion := "3.7.4"
ThisBuild / scalafmtOnCompile := true

Test / parallelExecution := true
Test / Keys.fork := true

Universal / javaOptions ++= Seq(
  "-Dpidfile.path=/dev/null",
  s"-Dgit.commit.hash=v${version.value}-snapshot-$shortCommitHash"
)

packageName := "genealogy"
dockerBaseImage := "eclipse-temurin:21"
dockerExposedPorts ++= Seq(9123)
dockerChmodType := DockerChmodType.UserGroupWriteExecute
dockerUsername := Some("pascal22p")
dockerCommands ++= Seq(
  Cmd("USER", "root"),
  ExecCmd("RUN", "apt-get", "update"),
  ExecCmd("RUN", "apt-get", "install", "-y", "graphviz"),
  ExecCmd("RUN", "apt-get", "clean"),
  ExecCmd("RUN", "rm", "-rf", "/var/lib/apt/lists/*"),
  Cmd("USER", "1001:0")
)
dockerBuildOptions ++= Seq("--load")

// Helper to get the short Git commit hash
def shortCommitHash: String = {
  sys.process.Process("git rev-parse --short HEAD").!!.trim
}

// Check if this is a manual release (e.g., via environment variable or Git tag)
def isRelease = Def.setting {
  // Option 1: Check for a RELEASE_BUILD environment variable set in CI
  sys.env.get("RELEASE_BUILD").contains("true") ||
    // Option 2: Check if the current commit is tagged with a version (requires sbt-git)
  (ThisBuild / git.gitCurrentTags).value.exists(_.startsWith("v"))
}

// Define the Docker version dynamically (snapshot by default)
Docker / version := {
  val baseVersion = version.value
  if (isRelease.value) baseVersion else s"$baseVersion-snapshot-${shortCommitHash}"
}

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
    (Docker / dockerAliases).value.foreach(
      alias =>
        Process("docker buildx build --platform=linux/arm64,linux/amd64 --push -t " +
          alias + " .", baseDirectory.value / "target" / "docker" / "stage").!
    )
  },
  Docker / dockerUsername := dockerUsername.value,
  Docker / maintainer := "genealogie@parois.net",
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
  .enablePlugins(SbtWeb)
  .settings(
    PlayKeys.playDefaultPort := 9123,
    libraryDependencies ++= LibDependencies.all,
    scoverageSettings,
    dockerBuildxSettings,
    Compile / compile / wartremoverErrors ++= Warts.allBut(DefaultArguments, ImplicitParameter, Overloading, Equals, Recursion, Any, Throw, SeqApply, Nothing, IterableOps, MutableDataStructures, Var),
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
      "-no-indent",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:noAutoTupling",
      "-language:strictEquality",
      "-Xkind-projector",
      "-Wvalue-discard",
      "-Wunused:all",
      "-Xfatal-warnings",
      //"-Yexplicit-nulls",
      "-Wsafe-init",
      "-Wconf:msg=unused import&src=html/.*:s",
      "-Wconf:msg=unused import&src=xml/.*:s",
      "-Wconf:src=routes/.*:s",
      "-Wconf:msg=Implicit parameters should be provided with a `using` clause&src=html/.*:s",
      "-Wconf:msg=Implicit parameters should be provided with a `using` clause&src=xml/.*:s",
      "-Wconf:msg=package scala contains object and package with same name.*:i"
    ),

    // Define Docker aliases (tags)
    Docker / dockerAliases := {
      val baseVersion = version.value
      val snapshotTag = s"v$baseVersion-snapshot-${shortCommitHash}"
      val releaseTag = s"v$baseVersion"

      if (isRelease.value) {
        Seq(
          DockerAlias(
            registryHost = (Docker / dockerRepository).value,
            username = dockerUsername.value,
            name = (Docker / packageName).value,
            tag = Some(releaseTag)
          ),
          DockerAlias(
            registryHost = (Docker / dockerRepository).value,
            username = dockerUsername.value,
            name = (Docker / packageName).value,
            tag = Some("latest")
          )
        )
      } else {
        Seq(
          DockerAlias(
            registryHost = (Docker / dockerRepository).value,
            username = dockerUsername.value,
            name = (Docker / packageName).value,
            tag = Some(snapshotTag)
          ),
          DockerAlias(
            registryHost = (Docker / dockerRepository).value,
            username = dockerUsername.value,
            name = (Docker / packageName).value,
            tag = Some("latest-snapshot")
          )
        )
      }
    }

  )

  Test / scalacOptions --= Seq("-language:strictEquality")

  