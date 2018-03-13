package org.mikesajak.commander

import java.util.zip.{ZipEntry => jZipEntry, ZipFile => jZipFile}

import org.mikesajak.commander.fs.{VDirectory, VFile, VPath}

trait ArchiveType

trait ArchiveHandler {
  def archiveType: ArchiveType
  def isArchive(file: VFile): Boolean
  def getArchiveFS(file: VFile): VDirectory
}


class ArchiveManager extends FileTypeDetector {
//  private var supportedArchives = Map[String, ArchiveHandler](
//    "zip" -> new ZipArchiveHandler
//  )
  private var archiveHandlers = List[ArchiveHandler]()

//  def registerArchiveHandler(extension: String, handler: ArchiveHandler): Unit =
//    supportedArchives += extension -> handler
  def registerArchiveHandler(handler: ArchiveHandler): Unit = {
    archiveHandlers ::= handler
}

  def isArchive(file: VFile): Boolean =
//    file.extension.exists(ext => supportedArchives.contains(ext))
    archiveHandlers.exists(handler => handler.isArchive(file))


  def getArchiveFS(file: VFile): VDirectory = {
    throw new IllegalArgumentException(s"Not an archive: $file")
  }

  override def detect(path: VPath): Option[FileType] = {
    val archiveType =
      path match {
        case file: VFile =>
          val hdl = archiveHandlers.find(h => h.isArchive(file))
          val r = hdl.map(h => h.archiveType)
          r
        case _ => None
      }
    archiveType.map(_ => FileType.ArchiveFile)
  }
}
