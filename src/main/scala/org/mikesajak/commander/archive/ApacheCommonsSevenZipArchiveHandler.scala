package org.mikesajak.commander.archive

import org.apache.commons.compress.archivers.ArchiveException
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.mikesajak.commander.fs.archive.CommonsArchiveRootDir
import org.mikesajak.commander.fs.local.LocalFile
import org.mikesajak.commander.fs.{VDirectory, VFile}

import scala.jdk.CollectionConverters._

class ApacheCommonsSevenZipArchiveHandler extends ArchiveHandler {
  private val archiveType: ArchiveType = ArchiveType.forExtension("7z")
  override val supportedArchives: Set[ArchiveType] = Set(archiveType)

  override def archiveType(file: VFile): Option[ArchiveType] =
    file match {
      case f : LocalFile => getSevenZFile(f).map(_ => archiveType)
      case _ => None
    }

  private def getSevenZFile(file: LocalFile) = try {
    Some(new SevenZFile(file.file))
  } catch {
    case _: ArchiveException => None
    case _: Exception => None
  }

  override def getArchiveRootDir(file: VFile): Option[VDirectory] = {
    file match {
      case f : LocalFile =>
        getSevenZFile(f)
            .map(sevenZFile => new CommonsArchiveRootDir(file, archiveType, sevenZFile.getEntries.asScala.toSeq))
      case _ => None
    }
  }

  override def toString: String = "Apache Commons 7Z archive handler"
}
