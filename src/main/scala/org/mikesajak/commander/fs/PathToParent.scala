package org.mikesajak.commander.fs

import java.time.Instant

/**
 * Created by mike on 26.10.14.
 */
class PathToParent(val currentDir: VDirectory) extends VDirectory {
  override val name = ".."

  def targetDir: VDirectory = currentDir.parent.get

  override val directory: VDirectory = currentDir

  override def fileSystem: FS = currentDir.fileSystem

  override def parent: Option[VDirectory] = currentDir.parent.get.parent

  override def absolutePath: String = currentDir.parent.get.absolutePath

  override def attributes: Attribs = currentDir.parent.get.attributes

  override def modificationDate: Instant = currentDir.parent.get.modificationDate

  override def size: Long = currentDir.parent.get.size

  override def children: Seq[VPath] = currentDir.parent.get.children

  override def childDirs: Seq[VDirectory] = currentDir.parent.get.childDirs

  override def childFiles: Seq[VFile] = currentDir.parent.get.childFiles

  override val updater: Option[VDirectoryUpdater] = None

  override def isParent(path: VPath): Boolean = currentDir.parent.get.isParent(path)

  override val exists: Boolean = true

  override def toString = s".. -> ($absolutePath)"
}
