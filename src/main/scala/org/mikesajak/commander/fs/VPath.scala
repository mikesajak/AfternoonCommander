package org.mikesajak.commander.fs

import java.io.{InputStream, OutputStream}
import java.time.Instant

import org.mikesajak.commander.util.PathUtils

import scala.collection.SortedSet

class Attribs(val values: SortedSet[Char]) {
  def this() = this(SortedSet.empty[Char])
  def this(attrs: Char*) = this(SortedSet(attrs: _*))

  def contains(attr: Char): Boolean = values.contains(attr)

  override def toString: String = (values foldLeft "")(_+_)
}

object Attribs {
  def builder() = new Builder()

  class Builder {
    private var attrs = List[Char]()

    def addAttrib(a: Char): Builder = {
      attrs :+= a
      this
    }

    def build() = new Attribs(SortedSet(attrs: _*))
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

  def mkChildDir(child: String): VDirectory
  def mkChildFile(child: String): VFile

  override val isDirectory = true
  override val directory: VDirectory = this
}



