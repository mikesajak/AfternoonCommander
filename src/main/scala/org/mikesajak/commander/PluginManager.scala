package org.mikesajak.commander

import org.mikesajak.commander.archive._
import org.mikesajak.commander.fs.FilesystemsManager

/**
  * Created by mike on 03.05.17.
  */
class PluginManager(fsMgr: FilesystemsManager, archiveManager: ArchiveManager) {

  def init(): Unit = {
    fsMgr.init()

    def archiveHandlers = detectArchiveHandlerPlugins()
    archiveHandlers.foreach { handler =>
      archiveManager.registerArchiveHandler(handler)
    }

  }

  def detectArchiveHandlerPlugins(): Seq[ArchiveHandler] = {
    Seq(new ApacheCommonsArchiveHandler,
        new ApacheCommonsSevenZipArchiveHandler,
        new XZArchiveHandler)
  }
}
