package org.mikesajak.commander

import com.google.inject.Inject
import org.mikesajak.commander.fs.FilesystemsManager
import org.mikesajak.commander.fs.zip.ZipArchiveHandler

/**
  * Created by mike on 03.05.17.
  */
class PluginManager @Inject() (fsMgr: FilesystemsManager, archiveManager: ArchiveManager) {

  def init(): Unit = {
    fsMgr.init()

    archiveManager.registerArchiveHandler(new ZipArchiveHandler)
  }
}
