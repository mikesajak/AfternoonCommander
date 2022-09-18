package org.mikesajak.commander.fs

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed abstract class Attrib(val symbol: String) extends EnumEntry
object Attrib extends Enum[Attrib] {
  val values: immutable.IndexedSeq[Attrib] = findValues

  case object Directory extends Attrib("d")
  case object Readable extends Attrib("r")
  case object Writable extends Attrib("w")
  case object Executable extends Attrib("x")
  case object Symlink extends Attrib("s")
  case object Hidden extends Attrib("h")
}

class Attribs(val values: Set[Attrib]) {
  def this() = this(Set.empty[Attrib])
  def this(attrs: Attrib*) = this(Set(attrs: _*))

  def contains(attr: Attrib): Boolean = values.contains(attr)
  def add(attr: Attrib) = new Attribs(values + attr)

  override def toString: String =
    Attrib.values.filter(values.contains)
          .map(_.symbol).foldLeft("")(_+_)
}

object Attribs {
  def builder() = new Builder()

  class Builder {
    private var attrs = List[Attrib]()

    def addAttrib(a: Attrib): Builder = {
      attrs :+= a
      this
    }

    def build() = new Attribs(Set(attrs: _*))
  }
}