package org.mikesajak.commander

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.fs.FilesystemsManager
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.ui._

class OperationMgr(statusMgr: StatusMgr,
                   resourceMgr: ResourceManager,
                   fsMgr: FilesystemsManager,
                   appController: ApplicationController,
                   copyOperationCtrl: CopyOperationCtrl,
                   mkDirOperationCtrl: MkDirOperationCtrl,
                   deleteOperationCtrl: DeleteOperationCtrl,
                   countDirStatsCtrl: CountDirStatsOperationCtrl,
                   settingsCtrl: SettingsCtrl,
                   findFilesCtrl: FindFilesCtrl,
                   propertiesCtrl: PropertiesCtrl) {
  private val logger = Logger(this.getClass)

  def handleView(): Unit = {
    logger.warn(s"handleView - Not implemented yet!")
  }

  def handleEdit(): Unit = {
    logger.warn(s"handleEdit - Not implemented yet!")
  }

  def handleCopy(): Unit = copyOperationCtrl.handleCopy()

  def handleMove(): Unit = {
    logger.warn(s"handleMove - Not implemented yet!")
  }

  def handleMkDir(): Unit = mkDirOperationCtrl.handleMkDir()

  def handleDelete(): Unit = deleteOperationCtrl.handleDelete()

  def handleCountDirStats(): Unit = countDirStatsCtrl.handleCountDirStats()

  def handleExit(): Unit = {
    appController.exitApplication()
  }

  def handleRefreshAction(): Unit = {
    logger.debug(s"Refreshing ${statusMgr.selectedPanel}, ${statusMgr.selectedTabManager.selectedTab.dir}")
    statusMgr.selectedTabManager.selectedTab.controller.reload()
  }

  def handleSettingsAction(): Unit = settingsCtrl.handleSettingsAction()

  def handleFindAction(): Unit = findFilesCtrl.handleFindAction()

  def handlePropertiesAction(): Unit = propertiesCtrl.handlePropertiesAction()

}
