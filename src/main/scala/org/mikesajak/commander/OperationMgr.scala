package org.mikesajak.commander

import org.mikesajak.commander.config.{ConfigKeys, Configuration}
import org.mikesajak.commander.fs.PathToParent
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.task.OperationType._
import org.mikesajak.commander.ui._
import scribe.Logging

import java.io.File
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.sys.process._
import scala.util.{Failure, Success}

class OperationMgr(statusMgr: StatusMgr,
                   appController: ApplicationController,
                   transferOperationCtrl: TransferOperationController,
                   mkDirOperationCtrl: MkDirOperationCtrl,
                   deleteOperationCtrl: DeleteOperationCtrl,
                   countDirStatsCtrl: CountDirStatsOperationCtrl,
                   settingsCtrl: SettingsCtrl,
                   findFilesCtrl: FindFilesCtrl,
                   propertiesCtrl: PropertiesCtrl,
                   config: Configuration,
                   executionContext: ExecutionContextExecutor) extends Logging {

  def handleView(): Unit = runExternalConfiguredAppWithSelectedFile(ConfigKeys.ToolsExternalViewer)

  def handleEdit(): Unit = runExternalConfiguredAppWithSelectedFile(ConfigKeys.ToolsExternalEditor)

  private def runExternalConfiguredAppWithSelectedFile(externalAppConfigKey: String): Unit = {
    val editorApp = config.stringProperty(externalAppConfigKey).value
                          .flatMap(editor => if (!editor.isBlank) Some(editor) else None)
                          .map(new File(_))
    editorApp match {
      case Some(editor) if editor.exists && editor.isFile && editor.canExecute =>
        runExternalAppWithSelectedFile(editor)

      case Some(file) =>
        logger.warn(s"Invalid external application $externalAppConfigKey defined: ${file.getAbsolutePath}")

      case None =>
        logger.warn(s"External application $externalAppConfigKey is not defined.")
    }
  }

  private def runExternalAppWithSelectedFile(viewer: File): Unit = {
    implicit val ec: ExecutionContextExecutor = executionContext

    val selectedTab = statusMgr.selectedTabManager.selectedTab
    val selectedFile = selectedTab.controller.selectedPaths
                                  .map(_._2)
                                  .filter(p => !p.isInstanceOf[PathToParent])
                                  .find(p => !p.isDirectory)
    selectedFile.foreach { selected =>
      logger.debug(s"Running external application: ${viewer.getAbsolutePath} ${selected.absolutePath}")
      Future {
        Seq(viewer.getAbsolutePath, selected.absolutePath).!!
      }.onComplete {
        case Success(output) =>
          logger.debug(s"Finished execution of external application. Output:\n$output")
        case Failure(exception) =>
          logger.warn(s"Error executing external application: ${viewer.getAbsolutePath}", exception)
      }
    }
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
