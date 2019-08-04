name := "AfternoonCommander"

version := "1.0"

scalaVersion := "2.12.8"

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

// scalafx (and fxml)
libraryDependencies += "org.scalafx" %% "scalafxml-core-sfx8" % "0.4"
libraryDependencies += "org.scalafx" %% "scalafxml-guice-sfx8" % "0.4"

// guice dependency injection
libraryDependencies += "com.google.inject" % "guice" % "4.2.2"
libraryDependencies += "net.codingwell" %% "scala-guice" % "4.2.6"

// https://mvnrepository.com/artifact/com.google.guava/guava
libraryDependencies += "com.google.guava" % "guava" % "26.0-jre"

// logging
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

// config
libraryDependencies += "com.typesafe" % "config" % "1.3.1"

// ICU4J https://mvnrepository.com/artifact/com.ibm.icu/icu4j
libraryDependencies += "com.ibm.icu" % "icu4j" % "60.2"

// detecting usb drives
libraryDependencies += "net.samuelcampos" % "usbdrivedetector" % "2.0.2"

libraryDependencies += "com.beachape" %% "enumeratum" % "1.5.13"

libraryDependencies += "com.typesafe.akka" % "akka-actor-typed_2.12" % "2.5.9"
