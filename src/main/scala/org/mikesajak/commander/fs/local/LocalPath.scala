package org.mikesajak.commander.fs.local

import java.io.File
import java.time.Instant

import org.mikesajak.commander.fs.Attrib._
import org.mikesajak.commander.fs.{Attrib, Attribs, VDirectory, VPath}

trait LocalPath extends VPath {

  val file: File
  val fileSystem: LocalFS

  override def name: String = file.getName

  override def absolutePath: String = file.getAbsolutePath

  override def size: Long = file.length()

  override def attributes: Attribs = {
    val b = Attribs.builder()

    if (file.isDirectory) b.addAttrib(Attrib.Directory)
    if (file.canRead) b.addAttrib(Readable)
    if (file.canWrite) b.addAttrib(Writable)
    if (file.canExecute) b.addAttrib(Executable)
    if (file.isHidden) b.addAttrib(Hidden)

    b.build()
  }

  override def modificationDate: Instant = Instant.ofEpochMilli(file.lastModified())

  override def isDirectory: Boolean = file.isDirectory

  override def parent: Option[VDirectory] = {
    if (file.getParent != null) Some(new LocalDirectory(file.getParentFile, fileSystem))
    else None
  }

  override def directory: VDirectory = {
    if (isDirectory) this.asInstanceOf[VDirectory] else parent.get
  }

  override def toString: String = s"${LocalFS.id}://$absolutePath"

  def canEqual(other: Any): Boolean //= other.isInstanceOf[LocalPath]

  override def equals(other: Any): Boolean = other match {
    case that: LocalPath =>
      (that canEqual this) &&
        file == that.file
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(file)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }

}
