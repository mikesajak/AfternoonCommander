package org.mikesajak.commander.fs

/**
 * Created by mike on 26.10.14.
 */
class PathToParent(curDir: VDirectory) extends VDirectory {
  override val name = ".."

  override def fileSystem: FS = curDir.fileSystem

  override def parent: Option[VDirectory] = curDir.parent.get.parent

  override def absolutePath = curDir.parent.get.absolutePath

  override def attribs = curDir.parent.get.attribs

  override def modificationDate = curDir.parent.get.modificationDate

  override def children: Seq[VPath] = curDir.parent.get.children
}
