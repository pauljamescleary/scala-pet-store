organization := "io.github.pauljamescleary"
name := "scala-pet-store"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.12.2"

resolvers += Resolver.sonatypeRepo("snapshots")

val Http4sVersion = "0.17.0-M2"

libraryDependencies ++= Seq(
 "org.http4s"     %% "http4s-blaze-server" % Http4sVersion,
 "org.http4s"     %% "http4s-circe"        % Http4sVersion,
 "org.http4s"     %% "http4s-dsl"          % Http4sVersion,
 // Optional for auto-derivation of JSON codecs
 "io.circe" %% "circe-generic" % "0.8.0",
 // Optional for string interpolation to JSON model
 "io.circe" %% "circe-literal" % "0.8.0",
 "ch.qos.logback" %  "logback-classic"     % "1.2.1"
)

enablePlugins(ScalafmtPlugin)