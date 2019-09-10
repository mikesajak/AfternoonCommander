package org.mikesajak.commander.archive

import org.apache.commons.compress.archivers._
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.mikesajak.commander.fs._
import org.mikesajak.commander.fs.archive.ArchiveRootDir
import org.mikesajak.commander.fs.local.LocalFile

import scala.annotation.tailrec
import scala.collection.JavaConverters._

class ApacheCommonsArchiveHandler extends ArchiveHandler {
  override def supportedArchives: Set[ArchiveType] =
    ArchiveStreamFactory.findAvailableArchiveInputStreamProviders().keySet().asScala.toSet
      .filter(ext => ext != "7z") // apache archive does not support 7z in streaming mode
      .map(ArchiveType.forExtension)

  override def archiveType(file: VFile): Option[ArchiveType] = try {
    Some(ArchiveStreamFactory.detect(file.inStream))
        .filter(ext => ext != "7z")
        .map(ArchiveType.forExtension)
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

class SevenZipArchiveHandler extends ArchiveHandler {
  private val archiveType: ArchiveType = ArchiveType.forExtension("7z")
  override val supportedArchives: Set[ArchiveType] = Set(archiveType)

  override def archiveType(file: VFile): Option[ArchiveType] =
    file match {
      case f : LocalFile => getSevenZFile(f).map(s7 => archiveType)
      case _ => None
    }

  private def getSevenZFile(file: LocalFile) = try {
    Some(new SevenZFile(file.file))
  } catch {
    case _: ArchiveException => None
    case _: Exception => None
  }

  override def getArchiveFS(file: VFile): Option[VDirectory] = {
    file match {
      case f : LocalFile =>
        getSevenZFile(f)
            .map(sevenZFile => new ArchiveRootDir(file, sevenZFile.getEntries.asScala.toSeq))
      case _ => None
    }
  }
}
