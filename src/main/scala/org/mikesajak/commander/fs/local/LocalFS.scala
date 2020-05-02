package org.mikesajak.commander.fs.local

import java.io.File

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.fs.{FS, VDirectory, VPath}

import scala.util.matching.Regex

/**
 * Created by mike on 25.10.14.
 */
object LocalFS {
  val id = "local"

  val PathPattern: Regex = "local://(.+)".r

  def mkLocalPathName(path: String) = s"$id://$path"

  def isAbsolutePathPattern(pathName: String): Boolean = pathName.startsWith("/") || pathName.startsWith("\\")
  def isPathNameDir(pathName: String): Boolean = pathName.endsWith("/") || pathName.endsWith("\\")

  def getPathSegments(pathName: String): IndexedSeq[String] = {
    val segments = pathName.split("[/\\\\]").toIndexedSeq
    segments.lastOption
            .map(last => last + (if (isPathNameDir(pathName)) "/" else ""))
            .map(last => segments.dropRight(1).map(_ + "/") :+ last)
            .getOrElse(IndexedSeq.empty)
  }
}

class LocalFS(private val rootFile: File, override val attributes: Map[String, String]) extends FS {
  private val logger = Logger[LocalFS]

  def this(rootFile: File, attribs: Seq[(String, String)]) = this(rootFile, attribs.toMap)

  override val id: String = LocalFS.id

  override def freeSpace: Long = rootFile.getFreeSpace
  override def totalSpace: Long = rootFile.getTotalSpace
  override def usableSpace: Long = rootFile.getUsableSpace

  override def rootDirectory: VDirectory = new LocalDirectory(rootFile, this)

  override def resolvePath(path: String, forceDir: Boolean): Option[VPath] = {
    path match {
      case LocalFS.PathPattern(p) =>
        logger.debug(s"Resolving valid path pattern $path")
        resolve(p, forceDir)
      case p if LocalFS.isAbsolutePathPattern(p) =>
        logger.debug(s"Resolving local absolute path $path")
        resolve(p, forceDir) // try also to resolve raw path
      case _ =>
        logger.info(s"Cannot resolve path: $path")
        None
    }
  }

  private def resolve(pathname: String, forceDir: Boolean) = {
    val file = new File(pathname)

    if (isChild(file, rootFile)) {
      val resolved =
        if (forceDir || file.exists() && file.isDirectory || LocalFS.isPathNameDir(pathname)) new LocalDirectory(file, this)
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
