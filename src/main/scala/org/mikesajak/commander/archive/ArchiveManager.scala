package org.mikesajak.commander.archive

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.fs.{VDirectory, VFile, VPath}
import org.mikesajak.commander.handler.FileHandler
import org.mikesajak.commander.{FileType, FileTypeDetector}

class ArchiveType(val extension: String, val description: String) {
  private def canEqual(other: Any): Boolean = other.isInstanceOf[ArchiveType]

  override def equals(other: Any): Boolean = other match {
    case that: ArchiveType =>
      (that canEqual this) &&
          extension == that.extension &&
          description == that.description
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(extension, description)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }

  override def toString = s"ArchiveType($extension, $description)"
}

object ArchiveType {
  def forExtension(ext: String) = new ArchiveType(ext.toLowerCase, s"${ext.toUpperCase()} archive")
}

trait ArchiveHandler {
  def supportedArchives: Set[ArchiveType]
  def archiveType(file: VFile): Option[ArchiveType]
  def getArchiveRootDir(file: VFile): Option[VDirectory]
}

class ArchiveManager extends FileTypeDetector with FileHandler {
  private val logger = Logger[ArchiveManager]
  private var archiveHandlers = List[ArchiveHandler]()

  def registerArchiveHandler(handler: ArchiveHandler): Unit = {
    logger.info(s"Loading archive handler: $handler, supported archives: " +
                  s"[${handler.supportedArchives.map(ar => s"${ar.description}(${ar.extension})").reduce(_ + ", " + _)}]")
    archiveHandlers ::= handler
  }

  def getArchiveFS(file: VFile): VDirectory = {
    throw new IllegalArgumentException(s"Not an archive: $file")
  }

  override def detect(path: VPath): Option[FileType] = {
    path match {
      case file: VFile if file.exists =>
        findArchiveHandlerByExt(file)
            .map(_ => FileType.ArchiveFile)

      case _ => None
    }
  }

  def findArchiveHandlerByExt(file: VFile): Option[ArchiveHandler] = {
    file.extension.map(_.toLowerCase) match {
      case Some(ext) =>
        archiveHandlers.find(handler => handler.supportedArchives.exists(archiveType => archiveType.extension == ext))
      case _ => None
    }
  }

  def findArchiveHandler(file: VFile): Option[ArchiveHandler] =
    if (file.exists)
      archiveHandlers.find(_.archiveType(file).isDefined)
    else None
}
