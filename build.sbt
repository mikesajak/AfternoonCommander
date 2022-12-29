import sbt.Keys.libraryDependencies

name := "AfternoonCommander"

version := "1.0"

scalaVersion := "2.13.7"

scalacOptions += "-Ymacro-annotations"

scalacOptions += "-deprecation"

// scalafx (and fxml)
// Add dependency on ScalaFX library
libraryDependencies += "org.scalafx" %% "scalafx" % "18.0.2-R29"

// Determine OS version of JavaFX binaries
lazy val osName = System.getProperty("os.name") match {
  case n if n.startsWith("Linux")   => "linux"
  case n if n.startsWith("Mac")     => "mac"
  case n if n.startsWith("Windows") => "win"
  case _ => throw new Exception("Unknown platform!")
}

lazy val javaFXModules = Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
libraryDependencies ++= javaFXModules.map( m => "org.openjfx" % s"javafx-$m" % "17.0.2" classifier osName)

libraryDependencies ++= { // scalafx (and fxml)
  val scalafxmlVersion = "0.5"
  Seq("org.scalafx" %% "scalafxml-core-sfx8" % scalafxmlVersion,
      "org.scalafx" % "scalafxml-guice-sfx8_2.13" % scalafxmlVersion)
  // todo: cleanup %% / %
}

// https://mvnrepository.com/artifact/org.controlsfx/controlsfx
libraryDependencies += "org.controlsfx" % "controlsfx" % "11.1.1"

// guice dependency injection
libraryDependencies ++= Seq(
  "com.google.inject" % "guice" % "5.1.0",
  "net.codingwell" %% "scala-guice" % "5.1.0"
)

// https://mvnrepository.com/artifact/com.google.guava/guava
libraryDependencies += "com.google.guava" % "guava" % "31.1-jre"

// logging - Scribe
libraryDependencies += "com.outr" %% "scribe" % "3.10.3"

// config
libraryDependencies += "com.typesafe" % "config" % "1.4.2"

// ICU4J https://mvnrepository.com/artifact/com.ibm.icu/icu4j
libraryDependencies += "com.ibm.icu" % "icu4j" % "71.1"

// detecting usb drives
libraryDependencies += "net.samuelcampos" % "usbdrivedetector" % "2.2.1"

libraryDependencies += "com.beachape" %% "enumeratum" % "1.7.0"

// Apache Tika - mime type detection and file content scanning lib
libraryDependencies ++= {
  val tikaVersion = "2.4.1"
  val bouncyCastleVersion = "1.71.1"
  Seq("org.apache.tika" % "tika-core" % tikaVersion,
      "org.apache.tika" % "tika-parsers" % tikaVersion,
      "org.xerial" % "sqlite-jdbc" % "3.39.3.0",
      "org.apache.pdfbox" % "jbig2-imageio" % "3.0.4",
      "com.github.jai-imageio" % "jai-imageio-core" % "1.4.0",
      "org.bouncycastle" % "bcprov-jdk18on" % bouncyCastleVersion,
      "org.bouncycastle" % "bcmail-jdk18on" % bouncyCastleVersion,
      "org.bouncycastle" % "bcpkix-jdk18on" % bouncyCastleVersion)
}

libraryDependencies += "org.codehaus.guessencoding" % "guessencoding" % "1.4"

// for apache commons archive formats
libraryDependencies += "org.apache.commons" % "commons-compress" % "1.21"

// for xz,7z
libraryDependencies += "org.tukaani" % "xz" % "1.9"
