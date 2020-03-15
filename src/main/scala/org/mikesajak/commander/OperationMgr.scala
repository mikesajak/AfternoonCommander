package org.mikesajak.commander

import java.awt.Desktop
import java.io.File

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.config.{ConfigKeys, Configuration}
import org.mikesajak.commander.fs.FilesystemsManager
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.task.OperationType._
import org.mikesajak.commander.ui._

class OperationMgr(statusMgr: StatusMgr,
                   resourceMgr: ResourceManager,
                   fsMgr: FilesystemsManager,
                   appController: ApplicationController,
                   transferOperationCtrl: TransferOperationController,
                   mkDirOperationCtrl: MkDirOperationCtrl,
                   deleteOperationCtrl: DeleteOperationCtrl,
                   countDirStatsCtrl: CountDirStatsOperationCtrl,
                   settingsCtrl: SettingsCtrl,
                   findFilesCtrl: FindFilesCtrl,
                   propertiesCtrl: PropertiesCtrl,
                   config: Configuration) {
  private val logger = Logger(this.getClass)

  def handleView(): Unit = {

    if (Desktop.isDesktopSupported) {
      val viewerFile = config.stringProperty(ConfigKeys.ToolsExternalViewer).value
                             .flatMap(viewer => if (!viewer.isBlank) Some(viewer) else None)
                             .map(new File(_))
      viewerFile match {
        case Some(file) if file.exists && file.isFile =>
          Desktop.getDesktop.open(file)

        case Some(file) =>
          logger.warn(s"Invalid external viewer defined: ${file.getAbsolutePath}")

        case None =>
          logger.warn("External viewer is not defined.")
      }
    }
  }

  def handleEdit(): Unit = {
    logger.warn(s"handleEdit - Not implemented yet!")
  }

  def handleCopy(): Unit = transferOperationCtrl.handleOperation(Copy)

  def handleMove(): Unit = transferOperationCtrl.handleOperation(Move)

  def handleMkDir(): Unit = mkDirOperationCtrl.handleMkDir()

  def handleDelete(): Unit = deleteOperationCtrl.handleDelete()

  def handleCountDirStats(): Unit = countDirStatsCtrl.handleCountDirStats()

  def handleExit(): Unit = appController.exitApplication()

  def handleRefreshAction(): Unit = {
    logger.debug(s"Refreshing ${statusMgr.selectedPanel}, ${statusMgr.selectedTabManager.selectedTab.dir}")
    statusMgr.selectedTabManager.selectedTab.controller.reload()
  }

  def handleSettingsAction(): Unit = settingsCtrl.handleSettingsAction()

  def handleFindAction(): Unit = findFilesCtrl.handleFindAction()

  def handlePropertiesAction(): Unit = propertiesCtrl.handlePropertiesAction()

}
