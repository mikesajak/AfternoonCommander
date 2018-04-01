package org.mikesajak.commander.fs.local

import java.io.File
import java.net.URI
import java.nio.file.{Files, Paths}
import java.nio.{file => jfile}
import java.{io => jio}

import org.mikesajak.commander.fs.{FS, VDirectory, VPath}

import scala.util.Try
import scala.util.matching.Regex

/**
 * Created by mike on 25.10.14.
 */
object LocalFS {
  val id = "local"

  val PathPattern: Regex = "local://(.+)".r

  def mkLocalPathName(path: String) = s"$id://$path"
}

class LocalFS(private val rootFile: File, override val attributes: Map[String, String]) extends FS {
  def this(rootFile: File, attribs: Seq[(String, String)]) = this(rootFile, attribs.toMap)

  override val id: String = LocalFS.id

  override def exists(path: VPath): Boolean = new jio.File(path.absolutePath).exists

  override def delete(path: VPath): Try[Boolean] = Try {
    val normalizedPath = path.absolutePath.replace('\\', '/')
    val fsPath = Paths.get(new URI(s"file:///$normalizedPath"))
    Files.deleteIfExists(fsPath)
  }

  override def create(path: VPath): Try[Boolean] = Try {
    val f = new jio.File(path.absolutePath)
    if (f.isDirectory) f.mkdirs()
    else f.createNewFile()
  }

  override def freeSpace: Long = rootFile.getFreeSpace
  override def totalSpace: Long = rootFile.getTotalSpace
  override def usableSpace: Long = rootFile.getUsableSpace

  override def rootDirectory: VDirectory = new LocalDirectory(rootFile, this)

  override def resolvePath(path: String): VPath =
    path match {
      case LocalFS.PathPattern(p) =>
        val file = new File(p)
        if (file.isDirectory) new LocalDirectory(file, this)
        else new LocalFile(file, this)
      case _ => throw new IllegalArgumentException(s"Provided path parameter is invalid (path=$path). LocalFS supports only local paths.")
    }

  override def toString = s"LocalFS($id, $rootDirectory, $attributes)"

  def canEqual(other: Any): Boolean = other.isInstanceOf[LocalFS]

  override def equals(other: Any): Boolean = other match {
    case that: LocalFS =>
      (that canEqual this) &&
        rootFile == that.rootFile
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(rootFile)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
