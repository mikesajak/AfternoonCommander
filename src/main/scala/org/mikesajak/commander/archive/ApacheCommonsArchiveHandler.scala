package org.mikesajak.commander.archive

import org.apache.commons.compress.archivers._
import org.mikesajak.commander.fs._
import org.mikesajak.commander.fs.archive.ArchiveRootDir

import scala.annotation.tailrec
import scala.collection.JavaConverters._

class ApacheCommonsArchiveHandler extends ArchiveHandler {
  override def supportedArchives: Set[ArchiveType] =
    ArchiveStreamFactory.findAvailableArchiveInputStreamProviders().keySet().asScala.toSet
      .map(ext => new ArchiveType(ext, s"${ext.toUpperCase()} archive"))

  override def archiveType(file: VFile): Option[ArchiveType] = try {
    Some(ArchiveStreamFactory.detect(file.inStream))
        .map(id => new ArchiveType(id.toLowerCase, s"${id.toUpperCase()} archive"))
  } catch {
    case _: ArchiveException => None
    case _: Exception => None
  }

  override def getArchiveFS(file: VFile): Option[VDirectory] = {
    val archiveEntries = readArchiveEntries(file)
    Some(new ArchiveRootDir(file, archiveEntries))
  }

  private def readArchiveEntries(archiveFile: VFile): List[ArchiveEntry] = {
    val archiveInputStream = new ArchiveStreamFactory().createArchiveInputStream(archiveFile.inStream)
    readArchiveEntries(archiveInputStream, List())
  }

  @tailrec
  private def readArchiveEntries(archiveInStream: ArchiveInputStream,
                                 entryList: List[ArchiveEntry]): List[ArchiveEntry] = {
    Option(archiveInStream.getNextEntry) match {
      case Some(entry) => readArchiveEntries(archiveInStream, entry:: entryList)
      case _ => entryList
    }
  }
}
