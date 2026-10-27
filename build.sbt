ThisBuild / scalaVersion := "2.13.14"
ThisBuild / organization := "dev.karn"

lazy val root = (project in file("."))
  .settings(
    name := "log-analytics-engine",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.18" % Test
  )
