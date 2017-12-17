package org.mikesajak.commander.fs.local

import java.io.File
import java.net.URI
import java.nio.file.{Files, Paths}
import java.nio.{file => jfile}
import java.{io => jio}

import org.mikesajak.commander.fs.{FS, VDirectory, VPath}

import scala.util.Try

/**
 * Created by mike on 25.10.14.
 */
object LocalFS {
  val id = "local"

  val PathPattern = "local://(.+)".r

  def mkLocalPathName(path: String) = s"$id://$path"
}

class LocalFS(rootFile: File) extends FS {
  override val id: String = LocalFS.id

  override def exists(path: VPath): Boolean = new jio.File(path.name).exists

  override def delete(path: VPath): Try[Boolean] = Try {
    val normalizedPath = path.absolutePath.replace('\\', '/')
    val fsPath = Paths.get(new URI(s"file:///$normalizedPath"))
    Files.deleteIfExists(fsPath)
  }

  override def create(path: VPath): Try[Boolean] = Try {
    val f = new jio.File(path.name)
    if (f.isDirectory) f.mkdirs()
    else f.createNewFile()
  }

  override def freeSpace: Long = ???

  override def rootDirectory: VDirectory = new LocalDirectory(rootFile, this)


  override def resolvePath(path: String): VPath =
    path match {
      case LocalFS.PathPattern(path) =>
        val file = new File(path)
        if (file.isDirectory) new LocalDirectory(file, this)
        else new LocalFile(file, this)
      case _ => throw new IllegalArgumentException(s"Provided path parameter is invalid (path=$path). LocalFS supports only local paths.")
    }
}
