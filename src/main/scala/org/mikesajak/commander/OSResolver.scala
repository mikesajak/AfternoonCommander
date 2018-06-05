package org.mikesajak.commander

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable


sealed abstract class OSType(val detectionPatterns: String*) extends EnumEntry
object OSType extends Enum[OSType] {
  val values: immutable.IndexedSeq[OSType] = findValues

  case object Windows extends OSType("win")
  case object Mac extends OSType("mac")
  case object Unix extends OSType("nix", "nux", "aix")
  case object Solaris extends OSType("sunos")
}

class OSResolver {
  lazy val getOSType: OSType = {
    val osName = System.getProperty("os.name").toLowerCase
    OSType.values.find(_.detectionPatterns.exists(pat => osName.contains(pat))).get
  }
}