// Makes our code tidy
addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.3.0")

// Revolver allows us to use re-start and work a lot faster!
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.0")

// Native Packager allows us to create standalone jar
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.2")

// Database migrations
addSbtPlugin("org.flywaydb" % "flyway-sbt" % "4.2.0")

// Documentation plugins
addSbtPlugin("org.tpolecat" % "tut-plugin" % "0.6.2")

addSbtPlugin("com.47deg"  % "sbt-microsites" % "0.7.15")

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.4")

resolvers += "Flyway".at("https://davidmweber.github.io/flyway-sbt.repo")

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"
