package org.mikesajak.commander.fs.local

import java.io.File

import org.mikesajak.commander.fs.VDirectory

/**
 * Created by mike on 26.10.14.
 */
class LocalDirectory(override val file: File, override val fileSystem: LocalFS)
    extends LocalPath with VDirectory {

  if (!file.isDirectory)
    throw new IllegalArgumentException(s"Cannot create LocalDirectory for param file=$file that is NOT a directory")

  override def children: Seq[LocalPath] = {
    val childFiles = file.listFiles()
    if (childFiles != null) childFiles.map { f =>
        if (f.isDirectory) new LocalDirectory(f, fileSystem)
        else new LocalFile(f, fileSystem)
      }
    else Seq.empty
  }

  override def mkChildDir(child: String): LocalDirectory = new LocalDirectory(new File(file.getAbsolutePath + File.separator + child), this.fileSystem)
  override def mkChildFile(child: String): LocalFile = new LocalFile(new File(file.getAbsolutePath + File.separator + child), this.fileSystem)
}
