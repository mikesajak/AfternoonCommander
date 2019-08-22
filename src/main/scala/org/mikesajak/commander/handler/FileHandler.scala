package org.mikesajak.commander.handler

import org.mikesajak.commander.fs.{VDirectory, VPath}

trait FileHandler

abstract class ContainerFileHandler(val path: VPath) extends FileHandler {
  def getContainerDir: VDirectory
}

class DefaultDirectoryHandler(override val path: VPath) extends ContainerFileHandler(path) {
  override def getContainerDir: VDirectory = path.asInstanceOf[VDirectory]
}

class VirtualDirectoryHandler(dir: VDirectory) extends ContainerFileHandler(dir) {
  override def getContainerDir: VDirectory = path.directory
}

abstract class ActionFileHandler(val path: VPath) extends FileHandler {
  def handle()
}


