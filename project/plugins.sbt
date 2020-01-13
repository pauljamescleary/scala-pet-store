// Makes our code tidy
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.3.0")

// Revolver allows us to use re-start and work a lot faster!
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

// Native Packager allows us to create standalone jar
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.5.2")

// Documentation plugins
addSbtPlugin("org.tpolecat" % "tut-plugin" % "0.6.13")

addSbtPlugin("com.47deg" % "sbt-microsites" % "1.1.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.3")
