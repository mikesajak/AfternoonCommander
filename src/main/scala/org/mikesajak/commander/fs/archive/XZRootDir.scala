package org.mikesajak.commander.fs.archive

import org.mikesajak.commander.fs._

import java.time.Instant

class XZRootDir(file: VFile) extends VDirectory {
  override val name: String = file.name
  override val parent: Option[VDirectory] = Some(file.directory)
  override val absolutePath: String = s"${file.absolutePath}/xz:/"
  override val modificationDate: Instant = file.modificationDate
  override val creationDate: Instant = file.creationDate
  override val accessDate: Instant = file.accessDate
  override val attributes: Attribs = file.attributes
  override val size = 1
  override val exists = true
  override val directory: XZRootDir = this

  override val childDirs: Seq[VDirectory] = Seq.empty
  override val childFiles: Seq[VFile] = Seq(new XZFile(file, this))

  override def permissions: AccessPermissions = file.permissions

  override def isParent(path: VPath): Boolean = path match {
    case xzFile: XZFile if xzFile.parent.contains(this) => true
    case _ => false
  }

  override val updater: Option[VDirectoryUpdater] = None

  override val fileSystem: FS = new XZFileSystem(this)
}
