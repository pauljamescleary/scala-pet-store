// Makes our code tidy
addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "0.2")

// Revolver allows us to use re-start and work a lot faster!
addSbtPlugin("io.spray" % "sbt-revolver" % "0.8.0")

// Native Packager allows us to create standalone jar
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.0")
