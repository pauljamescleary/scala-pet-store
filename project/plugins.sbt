// Makes our code tidy
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.0.6")

// Revolver allows us to use re-start and work a lot faster!
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

// Native Packager allows us to create standalone jar
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.4.1")

// Documentation plugins
addSbtPlugin("org.tpolecat" % "tut-plugin" % "0.6.13")

addSbtPlugin("com.47deg" % "sbt-microsites" % "0.9.6")

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.3")
