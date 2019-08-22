package org.mikesajak.commander.fs.local

import java.io._
import java.nio.file.{Files, Paths}

import org.mikesajak.commander.fs.VFile

class LocalFile(inputFile: File, override val fileSystem: LocalFS) extends LocalPath with VFile {

  if (inputFile.isDirectory)
    throw new IllegalArgumentException(s"Cannot create LocalFile for param file=$file that IS a directory")

  override val file: File = Paths.get(inputFile.getAbsolutePath).toFile

  override def inStream = new BufferedInputStream(Files.newInputStream(Paths.get(inputFile.getAbsolutePath)))

  override def outStream = new BufferedOutputStream(Files.newOutputStream(Paths.get(inputFile.getAbsolutePath)))

  override def canEqual(other: Any): Boolean = other.isInstanceOf[LocalFile]
}
