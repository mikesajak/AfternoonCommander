import sbt.CrossVersion
import sbt.Keys.libraryDependencies

name := "AfternoonCommander"

version := "1.0"

scalaVersion := "2.12.8"

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

// scalafx (and fxml)
// Add dependency on ScalaFX library
libraryDependencies += "org.scalafx" %% "scalafx" % "12.0.1-R17"

// Determine OS version of JavaFX binaries
lazy val osName = System.getProperty("os.name") match {
  case n if n.startsWith("Linux")   => "linux"
  case n if n.startsWith("Mac")     => "mac"
  case n if n.startsWith("Windows") => "win"
  case _ => throw new Exception("Unknown platform!")
}

lazy val javaFXModules = Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
libraryDependencies ++= javaFXModules.map( m => "org.openjfx" % s"javafx-$m" % "12.0.1" classifier osName)

libraryDependencies ++= { // scalafx (and fxml)
  val scalafxmlVersion = "0.4"
  Seq("org.scalafx" %% "scalafxml-core-sfx8" % scalafxmlVersion,
      "org.scalafx" % "scalafxml-guice-sfx8_2.12" % scalafxmlVersion)
  // todo: cleanup %% / %
}

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
