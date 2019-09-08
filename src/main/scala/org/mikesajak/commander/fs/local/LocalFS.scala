package org.mikesajak.commander.fs.local

import java.io.File

import org.mikesajak.commander.fs.{FS, VDirectory, VPath}

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

  override def freeSpace: Long = rootFile.getFreeSpace
  override def totalSpace: Long = rootFile.getTotalSpace
  override def usableSpace: Long = rootFile.getUsableSpace

  override def rootDirectory: VDirectory = new LocalDirectory(rootFile, this)

  override def resolvePath(path: String): Option[VPath] =
    path match {
      case LocalFS.PathPattern(p) => resolve(p)
      case p => resolve(p) // try also to resolve raw path
    }

  private def resolve(pathname: String) = {
    val file = new File(pathname)

    if (isChild(file, rootFile)) {
      val resolved =
        if (file.isDirectory) new LocalDirectory(file, this)
        else new LocalFile(file, this)
      Some(resolved)
    } else None
  }

  private def isChild(childFile: File, parentFile: File) = {
    val parentPath = parentFile.getAbsolutePath
    val childPath = childFile.getAbsolutePath
    childPath.startsWith(parentPath)
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
