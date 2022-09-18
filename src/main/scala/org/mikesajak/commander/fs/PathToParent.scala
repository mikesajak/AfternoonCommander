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

  override def parent: Option[VDirectory] = targetDir.parent

  override def absolutePath: String = targetDir.absolutePath

  override def attributes: Attribs = targetDir.attributes

  override def modificationDate: Instant = targetDir.modificationDate

  override def creationDate: Instant = targetDir.creationDate

  override def accessDate: Instant = targetDir.accessDate

  override def size: Long = targetDir.size

  override def children: Seq[VPath] = targetDir.children

  override def childDirs: Seq[VDirectory] = targetDir.childDirs

  override def childFiles: Seq[VFile] = targetDir.childFiles

  override val updater: Option[VDirectoryUpdater] = None

  override def isParent(path: VPath): Boolean = targetDir.isParent(path)

  override val exists: Boolean = true

  override def permissions: AccessPermissions = targetDir.permissions

  override def toString = s".. -> ($absolutePath)"
}
