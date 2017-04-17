name := "AfternoonCommander"

version := "1.0"

scalaVersion := "2.12.1"

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

// scalafx (and fxml)
libraryDependencies += "org.scalafx" %% "scalafxml-core-sfx8" % "0.3"
libraryDependencies += "org.scalafx" % "scalafxml-guice-sfx8_2.12" % "0.3"

// guice dependency injection
libraryDependencies += "com.google.inject" % "guice" % "4.1.0"
libraryDependencies += "net.codingwell" %% "scala-guice" % "4.1.0"

// logging
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.7"

// config
libraryDependencies += "com.typesafe" % "config" % "1.3.1"
