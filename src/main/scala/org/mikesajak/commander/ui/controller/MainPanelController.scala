package org.mikesajak.commander.ui.controller

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.OperationMgr
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.ui.Action
import org.mikesajak.commander.ui.controller.PanelId.{LeftPanel, RightPanel}
import org.mikesajak.commander.ui.keys.KeyActionMapper
import org.mikesajak.commander.ui.keys.KeyActionMapper.KeyInput
import scalafx.Includes._
import scalafx.scene.control.{SplitPane, TabPane}
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.Pane
import scalafxml.core.macros.{nested, sfxml}

/**
 * Created by mike on 25.10.14.
 */
@sfxml
class MainPanelController(dirsSplitPane: SplitPane,
                          leftTabPane: TabPane,
                          rightTabPane: TabPane,
                          mainPane: Pane,
                          @nested[DirPanelController] leftDirPanelController: DirPanelControllerIntf,
                          @nested[DirPanelController] rightDirPanelController: DirPanelControllerIntf,

                          statusMgr: StatusMgr,
                          operationMgr: OperationMgr,
                          keyActionMapper: KeyActionMapper) {
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
        case Action.Delete => operationMgr.handleDelete()
        case Action.Exit => operationMgr.handleExit()

        // Debug: temporary
        case Action.CountDirStats => operationMgr.handleCountDirStats()

        case Action.ShowProperties => operationMgr.handlePropertiesAction()

        case Action.Refresh => operationMgr.handleRefreshAction()
        case Action.FindFiles => operationMgr.handleFindAction()

        case Action.SwitchSelectedPanel => handleSwitchSelectedPanel()
      }
      ke.consume()
    }
  }

  private def handleSwitchSelectedPanel(): Unit = {
    val panelToSelect = PanelId.oppositePanel(statusMgr.selectedPanel)
    logger.debug(s"Switching panel ${statusMgr.selectedPanel} -> $panelToSelect")
    val panelCtrl = panelToSelect match {
      case PanelId.LeftPanel => leftDirPanelController
      case PanelId.RightPanel => rightDirPanelController
    }

    panelCtrl.requestFocus()
  }

}


