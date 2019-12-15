package org.mikesajak.commander.fs.archive

import java.io.{BufferedInputStream, InputStream}
import java.time.Instant

import org.apache.commons.compress.compressors.xz.XZUtils
import org.mikesajak.commander.fs._
import org.tukaani.xz.XZInputStream

class XZFile(file: VFile, parentDir: XZRootDir) extends VFile {

  override val name: String = XZUtils.getUncompressedFilename(file.name)

  override val parent: Option[VDirectory] = Some(parentDir)

  override val directory: VDirectory = parentDir

  override def absolutePath: String = s"${parentDir.absolutePath}/$name"

  override def modificationDate: Instant = file.modificationDate

  override def attributes: Attribs = file.attributes

  override def fileSystem: FS = parentDir.fileSystem

  override def size: Long = 0

  override val exists: Boolean = true

  override def inStream: InputStream = new BufferedInputStream(new XZInputStream(file.inStream))

  override def updater: Option[VFileUpdater] = None
}
