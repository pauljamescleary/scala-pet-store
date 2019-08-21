// Makes our code tidy
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.0.3")

// Revolver allows us to use re-start and work a lot faster!
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

// Native Packager allows us to create standalone jar
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.25")

// Database migrations
addSbtPlugin("io.github.davidmweber" % "flyway-sbt" % "5.2.0")

// Documentation plugins
addSbtPlugin("org.tpolecat" % "tut-plugin" % "0.6.12")

addSbtPlugin("com.47deg"  % "sbt-microsites" % "0.7.27")

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.3")

resolvers += "Flyway".at("https://davidmweber.github.io/flyway-sbt.repo")

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"
