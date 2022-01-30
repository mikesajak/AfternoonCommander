package org.mikesajak.commander.fs.archive

import org.apache.commons.compress.archivers.ArchiveEntry
import org.mikesajak.commander.fs._

import java.io.{BufferedInputStream, InputStream}
import java.time.Instant

class CommonsArchiveFile(archiveStreamProvider: ArchiveStreamProvider, archiveEntry: ArchiveEntry, parentDir: VDirectory) extends VFile {
  override val name: String = archiveEntry.getName.split("[/\\\\]").last

  override val parent: Option[VDirectory] = Some(parentDir)

  override val directory: VDirectory = parentDir

  override val absolutePath: String = s"${parentDir.absolutePath}/$name"

  override val modificationDate: Instant = Instant.ofEpochMilli(archiveEntry.getLastModifiedDate.getTime)

  override val creationDate: Instant = modificationDate

  override val accessDate: Instant = modificationDate

  override val attributes: Attribs = new Attribs(Attrib.Readable)

  override def size: Long = archiveEntry.getSize

  override val fileSystem: FS = parentDir.fileSystem

  override def inStream: InputStream =
    new BufferedInputStream(archiveStreamProvider.getStreamForEntry(archiveEntry))

  override val updater: Option[VFileUpdater] = None

  override val exists = true

  override val toString: String = name
}
