organization    := "io.github.pauljamescleary"
name            := "scala-pet-store"
version         := "0.0.1-SNAPSHOT"
scalaVersion    := "2.12.3"

resolvers += Resolver.sonatypeRepo("snapshots")

val CatsVersion       = "1.0.0-MF"
val CatsMtlVersion    = "0.0.2"
val CirceVersion      = "0.9.0-M1"
val DoobieVersion     = "0.5.0-M8"
val H2Version         = "1.4.196"
val Http4sVersion     = "0.18.0-M1"
val JodaTimeVersion   = "2.9.9"
val LogbackVersion    = "1.2.3"
val ScalaCheckVersion = "1.13.5"
val ScalaTestVersion  = "3.0.4"

libraryDependencies ++= Seq(
  "org.typelevel"  %% "cats-core"            % CatsVersion,
  "io.circe"       %% "circe-generic"        % CirceVersion,
  "io.circe"       %% "circe-literal"        % CirceVersion,
  "io.circe"       %% "circe-generic-extras" % CirceVersion,
  "io.circe"       %% "circe-optics"         % CirceVersion,
  "io.circe"       %% "circe-parser"         % CirceVersion,
  "org.tpolecat"   %% "doobie-core"          % DoobieVersion,
  "org.tpolecat"   %% "doobie-h2"            % DoobieVersion,
  "org.tpolecat"   %% "doobie-scalatest"     % DoobieVersion,
  "com.h2database" %  "h2"                   % H2Version,
  "org.http4s"     %% "http4s-blaze-server"  % Http4sVersion,
  "org.http4s"     %% "http4s-circe"         % Http4sVersion,
  "org.http4s"     %% "http4s-dsl"           % Http4sVersion,
  "ch.qos.logback" %  "logback-classic"      % LogbackVersion,
  "joda-time"      %  "joda-time"            % JodaTimeVersion,
  "org.scalacheck" %% "scalacheck"           % ScalaCheckVersion % Test,
  "org.scalatest"  %% "scalatest"            % ScalaTestVersion  % Test
)

enablePlugins(ScalafmtPlugin)
