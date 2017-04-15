package org.mikesajak.commander.fs.local

import java.io.File
import java.nio.{file => jfile}
import java.{io => jio}

import org.mikesajak.commander.fs.{VDirectory, FS, VPath}

/**
 * Created by mike on 25.10.14.
 */
object LocalFS {
  val id = "local"
}

class LocalFS(rootFile: File) extends FS {
  override val id: String = LocalFS.id

  override def exists(path: VPath): Boolean = new jio.File(path.name).exists

  override def delete(path: VPath): Boolean = new jio.File(path.name).delete()

  override def create(path: VPath): Boolean = {
    val f = new jio.File(path.name)
    if (f.isDirectory) f.mkdirs()
    else f.createNewFile()
  }

  override def freeSpace: Long = ???

  override def rootDirectory: VDirectory = new LocalDirectory(rootFile, this)


  override def resolvePath(path: String): VPath = {
    val file = new File(path)
    if (file.isDirectory) new LocalDirectory(file, this)
    else new LocalFile(file, this)
  }
}
