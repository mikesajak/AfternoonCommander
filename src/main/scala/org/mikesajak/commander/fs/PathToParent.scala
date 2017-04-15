package org.mikesajak.commander.fs

/**
 * Created by mike on 26.10.14.
 */
class PathToParent(targetDir: VDirectory) extends VDirectory {
  override val name = ".."

  override def fileSystem: FS = targetDir.fileSystem

  override def parent: Option[VDirectory] = targetDir.parent

  override def absolutePath = ???

  override def attribs = ???

  override def modificationDate = ???

  override def children: Seq[VPath] = ???
}
