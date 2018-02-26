package org.mikesajak.commander.fs.local
import java.io.File
import java.nio.file.Files

class Symlink(override val file: File, override val fileSystem: LocalFS) extends LocalPath {
  def target: LocalPath = {
    val targetFile = Files.readSymbolicLink(file.toPath).toFile
    if (targetFile.isDirectory) new LocalDirectory(targetFile, fileSystem)
    else new LocalFile(targetFile, fileSystem)
  }
}
