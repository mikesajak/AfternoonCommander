package org.mikesajak.commander.fs.local

import java.io._
import java.nio.file.{Files, Paths}
import java.time.Instant

import org.mikesajak.commander.fs.{VFile, VFileUpdater}

import scala.util.Try

class LocalFile(inputFile: File, override val fileSystem: LocalFS) extends LocalPath with VFile {

  if (inputFile.isDirectory)
    throw new IllegalArgumentException(s"Cannot create LocalFile for param file=${inputFile.getAbsolutePath} that IS a directory")

  override val file: File = Paths.get(inputFile.getAbsolutePath).toFile

  override def inStream = new BufferedInputStream(Files.newInputStream(Paths.get(inputFile.getAbsolutePath)))


  override lazy val updater: Option[VFileUpdater] = Some(new LocalFileUpdater(this))

  override def exists: Boolean = file.exists()

  override def canEqual(other: Any): Boolean = other.isInstanceOf[LocalFile]
}

class LocalFileUpdater(file: LocalFile) extends VFileUpdater {
  override def delete(): Try[Boolean] = Try {
    val fsPath = Paths.get(file.absolutePath)
    Files.deleteIfExists(fsPath)
  }

  override def create(): Try[Boolean] = Try {
    if (file.file.isDirectory) file.file.mkdirs()
    else file.file.createNewFile()
  }

  override def setModificationDate(date: Instant): Unit =
    file.file.setLastModified(date.getEpochSecond)

  override def outStream = new BufferedOutputStream(Files.newOutputStream(Paths.get(file.file.getAbsolutePath)))

}
