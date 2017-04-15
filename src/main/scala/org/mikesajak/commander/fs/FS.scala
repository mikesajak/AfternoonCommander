package org.mikesajak.commander.fs

trait FS {
  def id: String
  def rootDirectory: VDirectory

  def resolvePath(path: String): VPath

  def exists(path: VPath): Boolean
  def create(parent: VPath): Boolean
  def delete(path: VPath): Boolean

  def freeSpace: Long
}
