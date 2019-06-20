package org.mikesajak.commander.fs

import java.io.{InputStream, OutputStream}
import java.time.Instant

import enumeratum._
import org.mikesajak.commander.util.PathUtils

import scala.collection.immutable
import scala.language.implicitConversions

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

trait VPath {
  def name: String
  def parent: Option[VDirectory]
  def directory: VDirectory
  def absolutePath: String
  def segments: Seq[String] = PathUtils.collectParents(this)
  def modificationDate: Instant
  def attributes: Attribs
  def isDirectory: Boolean
  def isFile: Boolean = !isDirectory
  def fileSystem: FS
  def size: Long
}

trait VFile extends VPath {
  def extension: Option[String] = {
    val extPos = name.lastIndexOf('.')
    if (extPos != -1 && extPos < name.length - 1) {
      Some(name.slice(extPos+1, name.length))
    } else None
  }

  def inStream: InputStream
  def outStream: OutputStream
}

trait VDirectory extends VPath {
  def children: Seq[VPath]
  def childDirs: Seq[VDirectory]
  def childFiles: Seq[VFile]

  def isParent(path: VPath): Boolean

  def mkChildDir(child: String): VDirectory
  def mkChildFile(child: String): VFile

  override val isDirectory = true
  override val directory: VDirectory = this
}



