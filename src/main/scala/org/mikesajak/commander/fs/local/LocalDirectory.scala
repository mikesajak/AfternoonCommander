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

  override def children: Seq[LocalPath] =
    childDirs ++ childFiles

  override def childFiles: Seq[LocalFile] = {
    val files = file.listFiles()
    if (files != null)
      files.filter(f => !f.isDirectory)
        .map(f => new LocalFile(f, fileSystem))
    else Seq()
  }

  override def childDirs: Seq[LocalDirectory] ={
    val files = file.listFiles()
    if (files != null)
      files.filter(f => f.isDirectory)
          .map(d => new LocalDirectory(d, fileSystem))
    else Seq()
  }

  override def mkChildDir(child: String): LocalDirectory = {
    val newDir = new File(file, child)
    if (newDir.mkdir())
      new LocalDirectory(newDir, this.fileSystem)
    else throw new IllegalStateException(s"Couldnt't create new directory: $newDir")

  }
  override def mkChildFile(child: String): LocalFile = new LocalFile(new File(file.getAbsolutePath + File.separator + child), this.fileSystem)
}
