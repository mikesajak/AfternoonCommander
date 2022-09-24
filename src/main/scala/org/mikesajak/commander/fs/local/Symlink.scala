package org.mikesajak.commander.fs.local
import java.io.File
import java.nio.file.Files

class SymlinkFile(file: File, fileSystem: LocalFS) extends LocalFile(file, fileSystem) {
  if (file.isDirectory)
    throw new IllegalStateException(s"Cannot create SymlinkFile for directory target. file=$file")

  def target: LocalPath = {
    val symlinkNioPath = Files.readSymbolicLink(file.toPath)
    val targetFile = if (symlinkNioPath.isAbsolute) symlinkNioPath.toFile
                     else new File(file.getParentFile, symlinkNioPath.toString)
    new LocalFile(targetFile, fileSystem)
  }
}

class SymlinkDir(file: File, fileSystem: LocalFS) extends LocalDirectory(file, fileSystem) {
  if (!file.isDirectory)
    throw new IllegalStateException(s"Cannot create SymlinkDirectory for not directory target. file=$file")

  def target: LocalPath = {
    val symlinkNioPath = Files.readSymbolicLink(file.toPath)
    val targetFile = if (symlinkNioPath.isAbsolute) symlinkNioPath.toFile
                     else new File(file.getParentFile, symlinkNioPath.toString)
    new LocalDirectory(targetFile, fileSystem)
  }
}
