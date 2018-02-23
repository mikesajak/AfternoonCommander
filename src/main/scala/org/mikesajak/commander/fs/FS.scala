package org.mikesajak.commander.fs

import scala.util.Try

trait FS {
  def id: String
  def rootDirectory: VDirectory

  def resolvePath(path: String): VPath

  def exists(path: VPath): Boolean
  def create(parent: VPath): Try[Boolean]
  def delete(path: VPath): Try[Boolean]

  def freeSpace: Long
  def totalSpace: Long
  def usableSpace: Long
}

object FS {
  def rootDirOf(path: VPath): VDirectory = {
    var curDir = path.directory
    var parent= curDir.parent

    while (parent.isDefined) {
      curDir = parent.get
      parent= curDir.parent
    }
    curDir
  }
}
