package org.mikesajak.commander.fs.archive

import org.apache.commons.compress.archivers.{ArchiveEntry, ArchiveInputStream, ArchiveStreamFactory}
import org.mikesajak.commander.fs.VFile

class ArchiveStreamProvider(archiveFile: VFile) {
  def getStreamForEntry(archiveEntry: ArchiveEntry): ArchiveInputStream = {
    val archiveStream = new ArchiveStreamFactory().createArchiveInputStream(archiveFile.inStream)
    if (moveArchiveStreamToEntry(archiveStream, archiveEntry)) archiveStream
    else throw new ReadArchiveException(s"$archiveEntry not found in archive file $archiveFile")
  }

  private def moveArchiveStreamToEntry(archiveStream: ArchiveInputStream, archiveEntry: ArchiveEntry): Boolean = {
    var entry = archiveStream.getNextEntry
    while (entry != null && entry.getName != archiveEntry.getName) {
      entry = archiveStream.getNextEntry
    }
    entry != null
  }
}
