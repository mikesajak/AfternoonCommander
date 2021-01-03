package org.mikesajak.commander.fs.archive

import java.io.File

import org.apache.commons.compress.archivers.{ArchiveEntry, ArchiveInputStream, ArchiveStreamFactory}
import org.mikesajak.commander.archive.ApacheCommonsArchiveHandler
import org.mikesajak.commander.fs._
import org.mikesajak.commander.fs.local.{LocalFS, LocalFile}

import scala.annotation.tailrec
import scala.jdk.CollectionConverters._

object ArchiveTest {
  def main(args: Array[String]): Unit = {
    println(s"archive formats: ${ArchiveStreamFactory.findAvailableArchiveInputStreamProviders().asScala.keySet}")
//    val testFileName = "test/testDir6/test.zip"
//    val testFileName = "test/szczesliwego nowego jorku.odt.zip"
    val testFileName = "test/mediautil-1.zip"
    val testFile = new LocalFile(new File(testFileName),
                                 new LocalFS(new File("/home/mike"), List()))

    val h = new ApacheCommonsArchiveHandler()

    val archiveDir = h.getArchiveRootDir(testFile)

//    val archiveEntries = readArchiveEntries(testFile)
//    val archiveVDir = new ArchiveRootDir(testFile, archiveEntries)

    println(listContents(archiveDir.get))
  }

  def readArchiveEntries(archiveFile: VFile): List[ArchiveEntry] = {
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

  def listContents(directory: VDirectory, indent: String = ""): String = {
    val dirsResult = directory.childDirs.foldLeft("")((acc, dir) => s"$acc\n$indent[${dir.name}]${listContents(dir, indent + "  ")}")
    val filesResult = directory.childFiles.foldLeft("")((acc, file) => s"$acc\n$indent${file.name}")

    s"$dirsResult$filesResult"
  }
}






