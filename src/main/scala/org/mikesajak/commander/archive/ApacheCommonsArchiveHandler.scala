package org.mikesajak.commander.archive

import org.apache.commons.compress.archivers._
import org.mikesajak.commander.fs._
import org.mikesajak.commander.fs.archive.CommonsArchiveRootDir

import scala.annotation.tailrec
import scala.collection.JavaConverters._

class ApacheCommonsArchiveHandler extends ArchiveHandler {
  override def supportedArchives: Set[ArchiveType] =
    ArchiveStreamFactory.findAvailableArchiveInputStreamProviders().keySet().asScala
                        .map(_.toLowerCase)
                        .toSet
                        .filter(ext => ext != "7z") // apache archive does not support 7z in streaming mode
                        .map(ArchiveType.forExtension)

  override def archiveType(file: VFile): Option[ArchiveType] = try {
    Some(ArchiveStreamFactory.detect(file.inStream))
        .map(_.toLowerCase)
        .filter(ext => ext != "7z")
        .map(ArchiveType.forExtension)
  } catch {
    case _: ArchiveException => None
    case _: Exception => None
  }

  override def getArchiveRootDir(file: VFile): Option[VDirectory] = {
    archiveType(file).map { archType =>
      val archiveEntries = readArchiveEntries(file)
      new CommonsArchiveRootDir(file, archType, archiveEntries)
    }
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


