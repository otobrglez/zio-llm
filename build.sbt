import Dependencies.{scalafmt, *}

lazy val scala213Version = "2.13.17"
lazy val scala3Version   = "3.7.3"

lazy val supportedScalaVersions = List(scala213Version, scala3Version)

ThisBuild / resolvers += Resolver.sonatypeCentralSnapshots
ThisBuild / version            := "0.0.1"
ThisBuild / scalaVersion       := scala213Version
ThisBuild / crossScalaVersions := supportedScalaVersions
ThisBuild / organization       := "io.github.otobrglez"
ThisBuild / versionScheme      := Some("early-semver")

ThisBuild / scmInfo    := Some(
  ScmInfo(
    url("https://github.com/otobrglez/zio-llm"),
    "scm:git@github.com:otobrglez/zio-llm.git",
  ),
)
ThisBuild / developers := List(
  Developer(
    id = "otobrglez",
    name = "Oto Brglez",
    email = "otobrglez@gmail.com",
    url = url("https://github.com/otobrglez"),
  ),
)

ThisBuild / publishTo := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else localStaging.value
}

lazy val root = (project in file("."))
  .aggregate(`zio-llm`, `zio-llm-openai`, `zio-llm-openrouter`)
  .settings(
    name                := "zio-llm-root",
    publish / skip      := true,
    publishLocal / skip := true,
  )

lazy val `zio-llm` = (project in file("zio-llm"))
  .settings(
    name                 := "zio-llm",
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    libraryDependencies ++= Seq(
      `zio-test-sbt`,
      `zio-test`,
      `zio`,
    ),
    publishMavenStyle    := true,
    pomIncludeRepository := { _ => false },
  )

lazy val `zio-llm-openai` = (project in file("zio-llm-openai"))
  .dependsOn(`zio-llm`)
  .settings(
    name                 := "zio-llm-openai",
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    libraryDependencies ++= Seq(openai),
    publishMavenStyle    := true,
    pomIncludeRepository := { _ => false },
  )

lazy val `zio-llm-openrouter` = (project in file("zio-llm-openrouter"))
  .dependsOn(`zio-llm`)
  .settings(
    name                 := "zio-llm-openrouter",
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    libraryDependencies ++= Seq(
      `zio-http-testkit`,
      `zio-http`,
      `zio-test-sbt`,
      `zio-test`,
    ),
    publishMavenStyle    := true,
    pomIncludeRepository := { _ => false },
  )
