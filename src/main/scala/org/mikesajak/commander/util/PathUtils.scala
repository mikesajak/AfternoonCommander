package org.mikesajak.commander.util

import org.mikesajak.commander.fs.VPath

import scala.annotation.tailrec

object PathUtils {

  def shortenPathBy(path: String, length: Option[Int] = None): String = {
    val wishLen = length.getOrElse(10)
    shortenPathTo(path, wishLen+3)
  }

  def shortenPathTo(path: String, length: Int): String =
    if (path.length <= length) path
    else {
      val wishLength = math.min(length + 3, path.length - 4)
      val cutLength = path.length - wishLength
      s"...${path.substring(cutLength)}"
    }

  def splitNameByExt(n: String): (String, String) = {
    val idx = n.lastIndexOf('.')
    if (idx <= 0) (n, "") else (n.substring(0, idx), n.substring(idx+1))
  }

  @tailrec
  def depthToRoot(p: VPath, depth: Int = 0): Int =
    p.parent match {
      case Some(par) => depthToRoot(par, depth + 1)
      case _ => depth
    }

  @tailrec
  def findParent(path: VPath, parent: VPath): Option[VPath] = {
    if (path == parent) Some(path)
    else path.parent match {
      case Some(parentDir) => findParent(parentDir, parent)
      case None => None
    }
  }

  def pathSegments(name: String): Array[String] = name.split("[/\\\\]")
}
