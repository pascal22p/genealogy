name := """genealogy"""
organization := "parois.net"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)

scalaVersion := "3.4.2"

libraryDependencies += guice
libraryDependencies ++= Seq(
  jdbc,
  "org.mariadb.jdbc" % "mariadb-java-client" % "3.4.0",
  "org.playframework.anorm" %% "anorm" % "2.7.0",
  "org.typelevel"       %%  "cats-core"                         % "2.12.0",
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test
)

// Adds additional packages into Twirl

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "parois.net.binders._"
