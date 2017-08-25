organization := "io.github.pauljamescleary"
name := "scala-pet-store"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.12.3"

resolvers += Resolver.sonatypeRepo("snapshots")

val Http4sVersion = "0.18.0-M1"
val DoobieVersion = "0.5.0-M6"
val CirceVersion = "0.9.0-M1"

libraryDependencies ++= Seq(
 "org.http4s"     %% "http4s-blaze-server" % Http4sVersion,
 "org.http4s"     %% "http4s-circe"        % Http4sVersion,
 "org.http4s"     %% "http4s-dsl"          % Http4sVersion,
 "io.circe" %% "circe-generic" % CirceVersion,
 "io.circe" %% "circe-literal" % CirceVersion,
 "io.circe" %% "circe-generic-extras_sjs0.6" % CirceVersion,
 "io.circe" %% "circe-optics" % CirceVersion,
 "ch.qos.logback" %  "logback-classic"     % "1.2.1",
 "org.tpolecat" %% "doobie-core" % DoobieVersion,
 "org.tpolecat" %% "doobie-h2" % DoobieVersion,
 "org.tpolecat" %% "doobie-scalatest" % DoobieVersion,
 "com.h2database"            %  "h2"                             % "1.4.195",
 "joda-time" % "joda-time" % "2.9.9"
)

enablePlugins(ScalafmtPlugin)