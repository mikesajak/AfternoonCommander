package org.mikesajak.commander.fs.local

import java.io.File
import java.nio.file.Files
import java.time.Instant

import org.mikesajak.commander.fs.Attrib._
import org.mikesajak.commander.fs.{Attrib, Attribs, VDirectory, VPath}

trait LocalPath extends VPath {

  val file: File
  val fileSystem: LocalFS

  override def name: String = {
    if (file.getName.isEmpty) file.getAbsolutePath
    else file.getName
  }

  override def absolutePath: String = file.getAbsolutePath

  override def size: Long = file.length()

  override def attributes: Attribs = {
    val b = Attribs.builder()

    val path = file.toPath
    if (Files.isDirectory(path)) b.addAttrib(Attrib.Directory)
    if (Files.isReadable(path)) b.addAttrib(Readable)
    if (Files.isWritable(path)) b.addAttrib(Writable)
    if (Files.isExecutable(path)) b.addAttrib(Executable)
    if (Files.isHidden(path)) b.addAttrib(Hidden)
    if (Files.isSymbolicLink(path)) b.addAttrib(Symlink)

    b.build()
  }

  override def modificationDate: Instant = Instant.ofEpochMilli(file.lastModified())

  override def parent: Option[VDirectory] = {
    if (file.getParent != null) Some(new LocalDirectory(file.getParentFile, fileSystem))
    else None
  }

  override def directory: VDirectory = {
    if (isDirectory) this.asInstanceOf[VDirectory]
    else parent.get
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
