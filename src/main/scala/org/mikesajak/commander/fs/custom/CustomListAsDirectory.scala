package org.mikesajak.commander.fs.custom

import java.time.Instant

import org.mikesajak.commander.fs._

class CustomListAsDirectory(override val name: String, parentDir: VDirectory,
                            pathList: Seq[VPath]) extends VDirectory {
  override val parent: Option[VDirectory] = Some(parentDir)
  override val directory: VDirectory = this
  override def absolutePath: String = parentDir.absolutePath
  override def modificationDate: Instant = Instant.now()
  override val attributes: Attribs = new Attribs()
  override val isDirectory: Boolean = true
  override def fileSystem: FS = parentDir.fileSystem
  override val size: Long = 0

  override def children: Seq[VPath] = pathList
  def childDirs: Seq[VDirectory] = pathList.filter(_.isDirectory).map(_.asInstanceOf[VDirectory])
  def childFiles: Seq[VFile] = pathList.filter(_.isFile).map(_.asInstanceOf[VFile])

  def isParent(path: VPath): Boolean =
    if (pathList contains path) true
    else childDirs.collectFirst { case d => d.isParent(path) }
                  .getOrElse(false)

  override def mkChildDir(child: String): VDirectory = throw new IllegalStateException("Creating child directory not supported")
  override def mkChildFile(child: String): VFile = throw new IllegalStateException("Creating child file not supported")
}
