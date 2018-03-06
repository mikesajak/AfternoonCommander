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
  def collectParents(p: VPath, result: List[String] = Nil): List[String] = {
    val segment = p.parent.map(par => p.absolutePath.substring(par.absolutePath.length+1))
                  .getOrElse(p.absolutePath)

    p.parent match {
      case Some(par) =>
        val segment = p.absolutePath.substring(par.absolutePath.length+1)
        collectParents(par, segment :: result)
      case None =>
        p.absolutePath :: result
    }
  }

  @tailrec
  def findParent(path: VPath, parent: VPath): Option[VPath] = {
    if (path == parent) Some(path)
    else path.parent match {
      case Some(parentDir) => findParent(parentDir, parent)
      case None => None
    }
  }
}
