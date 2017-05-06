package org.mikesajak.commander

import org.mikesajak.commander.fs.FilesystemsManager
import org.mikesajak.commander.fs.zip.ZipArchiveHandler

/**
  * Created by mike on 03.05.17.
  */
class PluginManager(fsMgr: FilesystemsManager, archiveManager: ArchiveManager) {

  def init(): Unit = {
    fsMgr.init()

    archiveManager.registerArchiveHandler(new ZipArchiveHandler)
  }
}
