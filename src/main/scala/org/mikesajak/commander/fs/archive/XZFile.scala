package org.mikesajak.commander.fs.archive

import org.apache.commons.compress.compressors.xz.XZUtils
import org.mikesajak.commander.fs._
import org.tukaani.xz.XZInputStream

import java.io.{BufferedInputStream, InputStream}
import java.time.Instant

class XZFile(file: VFile, parentDir: XZRootDir) extends VFile {

  override val name: String = XZUtils.getUncompressedFilename(file.name)

  override val parent: Option[VDirectory] = Some(parentDir)

  override val directory: VDirectory = parentDir

  override def absolutePath: String = s"${parentDir.absolutePath}/$name"

  override def modificationDate: Instant = file.modificationDate

  override def creationDate: Instant = file.creationDate

  override def accessDate: Instant = file.accessDate

  override def attributes: Attribs = file.attributes

  override def fileSystem: FS = parentDir.fileSystem

  override def size: Long = 0

  override val exists: Boolean = true

  override def permissions: AccessPermissions = file.permissions

  override def inStream: InputStream = new BufferedInputStream(new XZInputStream(file.inStream))

  override def updater: Option[VFileUpdater] = None
}
