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
 "io.circe" %% "circe-generic" % "0.8.0",
 "io.circe" %% "circe-literal" % "0.8.0",
 "io.circe" %% "circe-generic-extras_sjs0.6" % "0.8.0",
 "io.circe" %% "circe-optics" % "0.8.0",
 "ch.qos.logback" %  "logback-classic"     % "1.2.1",
 "org.tpolecat" %% "doobie-core-cats" % "0.4.1",
 "org.tpolecat" %% "doobie-h2-cats" % "0.4.1",
 "org.tpolecat" %% "doobie-scalatest-cats" % "0.4.1",
 "com.h2database"            %  "h2"                             % "1.4.195"
)

enablePlugins(ScalafmtPlugin)