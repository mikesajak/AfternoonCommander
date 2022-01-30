package org.mikesajak.commander.fs

import enumeratum._

import java.io.{InputStream, OutputStream}
import java.time.Instant
import scala.collection.immutable
import scala.language.implicitConversions
import scala.util.Try

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
  def modificationDate: Instant
  def creationDate: Instant
  def accessDate: Instant
  def attributes: Attribs
  def isDirectory: Boolean
  def isFile: Boolean = !isDirectory
  def fileSystem: FS
  def size: Long
  def exists: Boolean
}

trait VFile extends VPath {
  def extension: Option[String] = {
    val extPos = name.lastIndexOf('.')
    if (extPos != -1 && extPos < name.length - 1) {
      Some(name.slice(extPos+1, name.length))
    } else None
  }

  override val isDirectory: Boolean = false

  def inStream: InputStream
  def updater: Option[VFileUpdater]
}

trait VFileUpdater {
  def create(): Try[Boolean]
  def delete(): Try[Boolean]
  def move(targetDir: VDirectory, name: Option[String]): Try[Boolean]
  def setModificationDate(date: Instant): Boolean
  def outStream: OutputStream
}

trait VDirectory extends VPath {
  def children: Seq[VPath] = childDirs ++ childFiles
  def childDirs: Seq[VDirectory]
  def childFiles: Seq[VFile]

  def getChild(name: String): Option[VPath] =
    children.find(child => child.name == name)

  def isParent(path: VPath): Boolean

  def updater: Option[VDirectoryUpdater]

  override val isDirectory = true
  override val directory: VDirectory = this
}

trait VDirectoryUpdater {
  def create(): Try[Boolean]
  def delete(): Try[Boolean]
  def move(targetDir: VDirectory, targetName: Option[String]): Try[Boolean]
  def setModificationDate(date: Instant): Boolean

  def mkChildDirPath(child: String): VDirectory
  def mkChildFilePath(child: String): VFile
}

