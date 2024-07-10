name := """genealogy"""
organization := "parois.net"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    PlayKeys.playDefaultPort := 9123,
    libraryDependencies ++= LibDependencies.all,
  )

scalaVersion := "3.4.2"



// Adds additional packages into Twirl

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "parois.net.binders._"
