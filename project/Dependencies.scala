import sbt.*

object Dependencies {
  val ZioVersion       = "2.1.21"
  val ZioSchemaVersion = "1.7.5"
  val ZioHttpVersion   = "3.5.1"
  val OpenAIVersion    = "4.3.0"

  val scalafmt         = "org.scalameta" %% "scalafmt-dynamic" % "3.10.0"
  val scalametaParsers = "org.scalameta" %% "parsers"          % "4.14.0"

  val zio                = "dev.zio" %% "zio"              % ZioVersion
  val `zio-streams`      = "dev.zio" %% "zio-streams"      % ZioVersion
  val `zio-schema`       = "dev.zio" %% "zio-schema"       % ZioSchemaVersion
  val `zio-schema-json`  = "dev.zio" %% "zio-schema-json"  % ZioSchemaVersion
  val `zio-test`         = "dev.zio" %% "zio-test"         % ZioVersion     % Test
  val `zio-test-sbt`     = "dev.zio" %% "zio-test-sbt"     % ZioVersion     % Test
  val `zio-http`         = "dev.zio" %% "zio-http"         % ZioHttpVersion
  val `zio-http-testkit` = "dev.zio" %% "zio-http-testkit" % ZioHttpVersion % Test

  val openai = "com.openai" % "openai-java" % OpenAIVersion
}
