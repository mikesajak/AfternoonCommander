package org.mikesajak.commander.handler

import org.mikesajak.commander.ApplicationController
import org.mikesajak.commander.archive.ArchiveManager
import org.mikesajak.commander.fs.{VDirectory, VFile, VPath}

class FileHandlerFactory(appCtrl: ApplicationController, archiveManager: ArchiveManager) {
  def getFileHandler(path: VPath): Option[FileHandler] = {
    path match {
      case d: VDirectory => Some(new DefaultDirectoryHandler(d))
      case f: VFile if archiveManager.findArchiveHandler(f).isDefined =>
        archiveManager.findArchiveHandler(f)
                      .flatMap(_.getArchiveFS(f))
                      .map(new VirtualDirectoryHandler(_))
      case f: VFile => Some(new DefaultOSActionFileHandler(f, appCtrl))
      case _ => None
    }
  }

  def getDefaultOSActionHandler(path: VPath): FileHandler =
    path match {
      case d: VDirectory => new DefaultDirectoryHandler(d)
      case f: VFile => new DefaultOSActionFileHandler(f, appCtrl)
    }
}
