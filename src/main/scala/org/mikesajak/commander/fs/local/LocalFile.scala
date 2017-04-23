package org.mikesajak.commander.fs.local

import java.io._

import org.mikesajak.commander.fs.VFile

class LocalFile(override val file: File, override val fileSystem: LocalFS) extends LocalPath with VFile {

  if (file.isDirectory)
    throw new IllegalArgumentException(s"Cannot create LocalFile for param file=$file that IS a directory")

  override def size: Long = file.length()

  override def extension: Option[String] = {
    val extPos = name.lastIndexOf('.')
    if (extPos != -1 && extPos < name.length - 1) {
      Some(name.slice(extPos+1, name.length))
    } else None
  }

  override def getInStream = new BufferedInputStream(new FileInputStream(file))

  override def getOutStream = new BufferedOutputStream(new FileOutputStream(file))
}
