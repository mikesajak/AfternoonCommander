package org.mikesajak.commander.fs.local

import java.io.File
import java.time.Instant

import org.mikesajak.commander.fs.{VDirectory, VPath}

trait LocalPath extends VPath {

  val file: File
  val fileSystem: LocalFS

  override def name: String = file.getName

  override def absolutePath: String = s"${LocalFS.id}://${file.getAbsolutePath}"

  override def attribs: String = {
    "" +
      (if (file.isDirectory) "d" else " ") +
      (if (file.canRead) "r" else " ") +
      (if (file.canWrite) "w" else " ") +
      (if (file.canExecute) "x" else " ") +
      (if (file.isHidden) "h" else " ")
  }

  override def modificationDate = Instant.ofEpochMilli(file.lastModified())

  override def isDirectory: Boolean = file.isDirectory

  override def parent: Option[VDirectory] = {
    if (file.getParent != null) Some(new LocalDirectory(file.getParentFile, fileSystem))
    else None
  }

  override def directory: VDirectory = {
    if (isDirectory) this.asInstanceOf[VDirectory] else parent.get
  }

  override def toString: String = absolutePath
}
