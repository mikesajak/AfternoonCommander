package org.mikesajak.commander.fs

import java.time.Instant

/**
 * Created by mike on 26.10.14.
 */
class PathToParent(val curDir: VDirectory) extends VDirectory {
  override val name = ".."

  def targetDir: VDirectory = curDir.parent.get

  override def fileSystem: FS = curDir.fileSystem

  override def parent: Option[VDirectory] = curDir.parent.get.parent

  override def absolutePath: String = curDir.parent.get.absolutePath

  override def attribs: String = curDir.parent.get.attribs

  override def modificationDate: Instant = curDir.parent.get.modificationDate

  override def children: Seq[VPath] = curDir.parent.get.children

  override def childDirs: Seq[VDirectory] = curDir.parent.get.childDirs

  override def childFiles: Seq[VFile] = curDir.parent.get.childFiles

  override def mkChildDir(child: String) = throw new UnsupportedOperationException(s"Create child dir not supported on this wrapper directory")

  override def mkChildFile(child: String) = throw new UnsupportedOperationException(s"Create child file not supported on this wrapper directory")
}
