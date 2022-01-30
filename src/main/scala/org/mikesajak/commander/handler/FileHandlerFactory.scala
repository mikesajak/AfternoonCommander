package org.mikesajak.commander.handler

import org.mikesajak.commander.ApplicationController
import org.mikesajak.commander.archive.ArchiveManager
import org.mikesajak.commander.fs.{VDirectory, VFile, VPath}

import scala.concurrent.ExecutionContextExecutor

class FileHandlerFactory(appCtrl: ApplicationController, archiveManager: ArchiveManager,
                         executionContextExecutor: ExecutionContextExecutor) {
  def getFileHandler(path: VPath): Option[FileHandler] = {
    path match {
      case d: VDirectory => Some(new DefaultDirectoryHandler(d))
      case f: VFile if archiveManager.findArchiveHandler(f).isDefined =>
        archiveManager.findArchiveHandler(f)
                      .flatMap(_.getArchiveRootDir(f))
                      .map(new VirtualDirectoryHandler(_))
      case f: VFile => Some(new DefaultOSActionFileHandler(f, appCtrl, executionContextExecutor))
      case _ => None
    }
  }

  def getDefaultOSActionHandler(path: VPath): FileHandler =
    new DefaultOSActionFileHandler(path, appCtrl, executionContextExecutor)
}
