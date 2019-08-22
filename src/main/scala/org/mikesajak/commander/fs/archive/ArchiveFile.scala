package org.mikesajak.commander.fs.archive

import java.io.{InputStream, OutputStream}
import java.time.Instant

import org.apache.commons.compress.archivers.ArchiveEntry
import org.mikesajak.commander.fs._

class ArchiveFile(archiveEntry: ArchiveEntry, parentDir: VDirectory) extends VFile {
  override val name: String = archiveEntry.getName.split("[/\\\\]").last

  override val parent: Option[VDirectory] = Some(parentDir)

  override val directory: VDirectory = parentDir

  override val absolutePath: String = archiveEntry.getName // TODO

  override val modificationDate: Instant = Instant.ofEpochMilli(archiveEntry.getLastModifiedDate.getTime)

  override val attributes: Attribs = new Attribs(Attrib.Readable)

  override def size: Long = archiveEntry.getSize

  override val fileSystem: FS = parentDir.fileSystem

  override def inStream: InputStream = throw new UnsupportedOperationException// TODO

  override def outStream: OutputStream = throw new UnsupportedOperationException // TODO

  override def toString = name

}
