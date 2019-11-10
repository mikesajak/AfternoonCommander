package org.mikesajak.commander.fs.local

import java.io.File
import java.nio.file.{Files, Paths}
import java.time.Instant

import org.mikesajak.commander.fs.{VDirectory, VDirectoryUpdater, VPath}

import scala.util.Try

/**
 * Created by mike on 26.10.14.
 */
class LocalDirectory(override val file: File, override val fileSystem: LocalFS)
    extends LocalPath with VDirectory {

  if (!file.isDirectory)
    throw new IllegalArgumentException(s"Cannot create LocalDirectory for param file=$file that is NOT a directory")

  override def childFiles: Seq[LocalFile] = {
    val files = file.listFiles()
    if (files != null)
      files.filter(f => !f.isDirectory).map(resolveFile)
    else Seq()
  }

  override def isParent(path: VPath): Boolean =
    path.absolutePath startsWith absolutePath

  private def resolveFile(file: File) = {
    if (Files.isSymbolicLink(file.toPath)) new SymlinkFile(file, fileSystem)
    else new LocalFile(file, fileSystem)
  }

  override def childDirs: Seq[LocalDirectory] ={
    val files = file.listFiles()
    if (files != null)
      files.filter(f => f.isDirectory).map(resolveDir)
    else Seq()
  }

  private def resolveDir(file: File) = {
    if (Files.isSymbolicLink(file.toPath)) new SymlinkDir(file, fileSystem)
    else new LocalDirectory(file, fileSystem)
  }

  override def exists: Boolean = file.exists()

  override lazy val updater = Some(new LocalDirectoryUpdater(this))

  override def canEqual(other: Any): Boolean = other.isInstanceOf[LocalDirectory]
}

class LocalDirectoryUpdater(dir: LocalDirectory) extends VDirectoryUpdater {
  override def mkChildDir(child: String): LocalDirectory = {
    val newDir = new File(dir.file, child)
    if (newDir.mkdir())
      new LocalDirectory(newDir, dir.fileSystem)
    else throw new IllegalStateException(s"Couldnt't create new directory: $newDir")
  }

  override def mkChildFile(child: String): LocalFile =
    new LocalFile(new File(dir.file.getAbsolutePath + File.separator + child), dir.fileSystem)

  override def setModificationDate(date: Instant): Unit =
    dir.file.setLastModified(date.toEpochMilli)

  override def delete(): Try[Boolean] = Try {
    val fsPath = Paths.get(dir.absolutePath)
    Files.deleteIfExists(fsPath)
  }

  override def create(): Try[Boolean] = Try {
    if (dir.file.isDirectory) dir.file.mkdirs()
    else dir.file.createNewFile()
  }

  override def move(targetDir: VDirectory, targetName: Option[String]): Try[Boolean] = Try {
    targetDir match {
      case targetLocalDir: LocalDirectory =>
        val targetPath = Paths.get(targetDir.absolutePath, targetName.getOrElse(dir.name))
        dir.file.renameTo(targetPath.toFile)
      case _ => false
    }
  }

  case class DirectoryMoveException(message: String, cause: Exception) extends Exception(message, cause) {
    def this(message: String) = this(message, this)
    def this(cause: Exception) = this(null, cause)
  }
}
