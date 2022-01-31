import sbt.Keys.libraryDependencies

name := "AfternoonCommander"

version := "1.0"

scalaVersion := "2.13.7"

scalacOptions += "-Ymacro-annotations"

// scalafx (and fxml)
// Add dependency on ScalaFX library
libraryDependencies += "org.scalafx" %% "scalafx" % "16.0.0-R22"

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
  val scalafxmlVersion = "0.5"
  Seq("org.scalafx" %% "scalafxml-core-sfx8" % scalafxmlVersion,
      "org.scalafx" % "scalafxml-guice-sfx8_2.13" % scalafxmlVersion)
  // todo: cleanup %% / %
}

// https://mvnrepository.com/artifact/org.controlsfx/controlsfx
libraryDependencies += "org.controlsfx" % "controlsfx" % "11.1.0"

// guice dependency injection
libraryDependencies ++= Seq(
  "com.google.inject" % "guice" % "5.0.1",
  "net.codingwell" %% "scala-guice" % "5.0.0"
)

// https://mvnrepository.com/artifact/com.google.guava/guava
libraryDependencies += "com.google.guava" % "guava" % "30.1.1-jre"

// logging - Scribe
libraryDependencies += "com.outr" %% "scribe" % "3.3.3"

// config
libraryDependencies += "com.typesafe" % "config" % "1.3.1"

// ICU4J https://mvnrepository.com/artifact/com.ibm.icu/icu4j
libraryDependencies += "com.ibm.icu" % "icu4j" % "60.2"

// detecting usb drives
libraryDependencies += "net.samuelcampos" % "usbdrivedetector" % "2.1.1"

libraryDependencies += "com.beachape" %% "enumeratum" % "1.6.1"

libraryDependencies += "com.typesafe.akka" % "akka-actor-typed_2.12" % "2.6.1"

// Apache Tika - mime type detection and file content scanning lib
libraryDependencies ++= {
  val tikaVersion = "1.26"
  val bouncyCastleVersion = "1.68"
  Seq("org.apache.tika" % "tika-core" % tikaVersion,
      "org.apache.tika" % "tika-parsers" % tikaVersion,
      "org.xerial" % "sqlite-jdbc" % "3.34.0",
      "org.apache.pdfbox" % "jbig2-imageio" % "3.0.3",
      "com.github.jai-imageio" % "jai-imageio-core" % "1.4.0",
      "org.bouncycastle" % "bcprov-jdk15on" % bouncyCastleVersion,
      "org.bouncycastle" % "bcmail-jdk15on" % bouncyCastleVersion,
      "org.bouncycastle" % "bcpkix-jdk15on" % bouncyCastleVersion)
}

// for apache commons archive formats
libraryDependencies += "org.apache.commons" % "commons-compress" % "1.20"

// for xz,7z
libraryDependencies += "org.tukaani" % "xz" % "1.9"
