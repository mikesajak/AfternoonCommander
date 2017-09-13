package org.mikesajak.commander.fs.zip

import java.io.{InputStream, OutputStream}
import java.time.Instant
import java.util.zip.{ZipEntry => jZipEntry, ZipFile => jZipFile}

import org.mikesajak.commander.fs.{FS, VDirectory, VFile, VPath}
import org.mikesajak.commander.{ArchiveFile, ArchiveHandler}

import scala.collection.JavaConverters._

/**
  * Created by mike on 03.05.17.
  */

case object ZipFile extends ArchiveFile

class ZipArchiveHandler extends ArchiveHandler {
  override val archiveType: ArchiveFile = ZipFile

  override def isArchive(file: VFile): Boolean = {
    val ext = file.extension
    ext.contains("zip")
    //      try {
    //        val zipFile = new ZipFile(file.absolutePath)
    //        true
    //      } catch {
    //        case e: Exception => false
    //      }
    //    else false
  }

  override def getArchiveFS(file: VFile): VDirectory = {
    val zipFile = new jZipFile(file.absolutePath)
    new ZipRoot(file.parent.get, zipFile)
  }

}

class ZipRoot(parentDir: VDirectory, zipFile: jZipFile) extends VDirectory {

  override def name: String = "/"
  override def parent: Option[VDirectory] = Some(parentDir)
  override def absolutePath: String = parentDir.absolutePath + "/zip://" + zipFile.getName
  override def modificationDate: Instant = Instant.now()
  override def attribs: String = "r"

  override def children: Seq[VPath] = {
    zipFile.entries().asScala
      .filter(ze => s"/${ze.getName}".startsWith(name))
      .map(ze => if (ze.isDirectory) new ZipDir(this, zipFile, ze) else new ZipInternalFile(this, zipFile, ze))
      .toSeq
  }
  override def fileSystem: FS = ???

  override def mkChildDir(child: String): Nothing = ???

  override def mkChildFile(child: String): Nothing = ???
}

class ZipDir(parentDir: VDirectory, zipFile: jZipFile, zipEntry: jZipEntry) extends VDirectory {

  override def name: String = "/" + zipEntry.getName
  override def parent: Option[VDirectory] = Some(parentDir)
  override def absolutePath: String = s"${ZipFS.id}://${zipFile.getName}/$zipEntry"

  override def children: Seq[VPath] = ???

  override def modificationDate: Instant = zipEntry.getLastModifiedTime.toInstant

  override def attribs: String = "r"

  override def fileSystem: FS = ???

  override def mkChildDir(child: String): Nothing = ???

  override def mkChildFile(child: String): Nothing = ???
}

class ZipInternalFile(parentDir: VDirectory, zipFile: jZipFile, zipEntry: jZipEntry) extends VFile {
  override def size: Long = zipEntry.getSize

  override def inStream: InputStream = zipFile.getInputStream(zipEntry)

  override def outStream: OutputStream =
    throw new UnsupportedOperationException(s"Cannot get output stream of zip file: $zipFile")

  override def name: String = "/" + zipEntry.getName

  override def parent: Option[VDirectory] = Some(parentDir)

  override def directory: VDirectory = parentDir

  override def absolutePath: String = zipEntry.getName

  override def modificationDate: Instant = zipEntry.getLastModifiedTime.toInstant

  override def attribs: String = "r"

  override def isDirectory: Boolean = false

  override def fileSystem: FS = ???
}

object ZipFS {
  val id = "zip"
}

class ZipFS extends FS {
  override def id: String = ZipFS.id

  override def rootDirectory: VDirectory = ???

  override def resolvePath(path: String): VPath = ???

  override def exists(path: VPath): Boolean = ???

  override def create(parent: VPath): Boolean = ???

  override def delete(path: VPath): Boolean = ???

  override def freeSpace: Long = ???
}
