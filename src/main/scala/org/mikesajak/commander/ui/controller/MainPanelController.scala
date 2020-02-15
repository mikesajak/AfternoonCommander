package org.mikesajak.commander.ui.controller

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.fs.VDirectory
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.ui.Action
import org.mikesajak.commander.ui.controller.DirViewEvents.{ChangeDirRequest, DriveSelectionRequest, FocusRequest, NewTabRequest}
import org.mikesajak.commander.ui.controller.PanelId.{LeftPanel, RightPanel}
import org.mikesajak.commander.ui.keys.KeyActionMapper
import org.mikesajak.commander.ui.keys.KeyActionMapper.KeyInput
import org.mikesajak.commander.{EventBus, OperationMgr}
import scalafx.Includes._
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.Pane
import scalafxml.core.macros.{nested, sfxml}

/**
 * Created by mike on 25.10.14.
 */
@sfxml
class MainPanelController(mainPane: Pane,
                          @nested[DirPanelController] leftDirPanelController: DirPanelControllerIntf,
                          @nested[DirPanelController] rightDirPanelController: DirPanelControllerIntf,

                          statusMgr: StatusMgr,
                          operationMgr: OperationMgr,
                          keyActionMapper: KeyActionMapper,
                          eventBus: EventBus) {
  private val logger = Logger[MainPanelController]

  leftDirPanelController.init(LeftPanel)
  rightDirPanelController.init(RightPanel)

  statusMgr.selectedPanel = LeftPanel

  mainPane.filterEvent(KeyEvent.KeyPressed) { ke: KeyEvent =>
    for (action <- keyActionMapper.actionForKey(KeyInput(ke))) {
      action match {
        case Action.View => operationMgr.handleView()
        case Action.Edit => operationMgr.handleEdit()
        case Action.Copy => operationMgr.handleCopy()
        case Action.Move => operationMgr.handleMove()
        case Action.MkDir => operationMgr.handleMkDir()
        case Action.Delete => operationMgr.handleDelete()
        case Action.Exit => operationMgr.handleExit()

        // Debug: temporary
        case Action.CountDirStats => operationMgr.handleCountDirStats()

        case Action.ShowProperties => operationMgr.handlePropertiesAction()

        case Action.Refresh => operationMgr.handleRefreshAction()
        case Action.FindFiles => operationMgr.handleFindAction()

        case Action.SwitchSelectedPanel => handleSwitchSelectedPanel()

        case Action.ShowDrivesLeftPanel => handleShowDrives(PanelId.LeftPanel)
        case Action.ShowDrivesRightPanel => handleShowDrives(PanelId.RightPanel)

        case Action.NewTab => handleNewTab()

        case Action.SelectCurrentDirInOtherPanel => handleSelectCurrentDirInOtherPanel()
        case Action.SelectTargetDirInOtherPanel => handleSelectTargetDirInOtherPanel()
      }
      ke.consume()
    }
  }

  private def handleSwitchSelectedPanel(): Unit = {
    val panelToSelect = PanelId.oppositePanel(statusMgr.selectedPanel)
    logger.debug(s"Switching panel ${statusMgr.selectedPanel} -> $panelToSelect")
    eventBus.publish(FocusRequest(panelToSelect))
  }

  private def handleShowDrives(panelId: PanelId): Unit = {
    logger.debug(s"Show drives selection $panelId")
    eventBus.publish(DriveSelectionRequest(panelId))
  }

  private def handleNewTab(): Unit = {
    val panelId = statusMgr.selectedPanel
    logger.debug(s"Adding new tab $panelId")
    eventBus.publish(NewTabRequest(panelId))
  }

  private def handleSelectCurrentDirInOtherPanel(): Unit =
    handleSelectDirInOtherPanel(statusMgr.selectedTabManager.selectedTab.dir)

  private def handleSelectTargetDirInOtherPanel(): Unit = {
    val dir = statusMgr.selectedTabManager.selectedTab.controller.selectedPaths.headOption
                                .map(_.directory)
                                .getOrElse(statusMgr.selectedTabManager.selectedTab.dir)
    handleSelectDirInOtherPanel(dir)
  }

  private def handleSelectDirInOtherPanel(dir: VDirectory): Unit = {
    val panelId = statusMgr.selectedPanel
    val otherPanelId = statusMgr.unselectedPanel

    logger.debug(s"Select dir in other panel, this panelId=$panelId, other panelId=$otherPanelId, dir=$dir")
    eventBus.publish(ChangeDirRequest(otherPanelId, dir))
  }
}


