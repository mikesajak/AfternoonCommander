package org.mikesajak.commander.archive

import org.mikesajak.commander.fs.archive.XZRootDir
import org.mikesajak.commander.fs.{VDirectory, VFile}
import org.tukaani.xz.XZInputStream

class XZArchiveHandler extends ArchiveHandler {
  private val archiveType = ArchiveType.forExtension("xz")
  override val supportedArchives: Set[ArchiveType] = Set(archiveType)

  override def archiveType(file: VFile): Option[ArchiveType] = try {
    val xzInStream = new XZInputStream(file.inStream)
    xzInStream.readNBytes(1)
    Some(archiveType)
  } catch {
    case _: Exception => None
  }

  override def getArchiveRootDir(file: VFile): Option[VDirectory] =
    archiveType(file).map { _ => new XZRootDir(file) }
}





