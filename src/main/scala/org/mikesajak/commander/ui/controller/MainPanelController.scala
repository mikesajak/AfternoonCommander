package org.mikesajak.commander.ui.controller

import org.mikesajak.commander.OperationMgr
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.ui.controller.PanelId.{LeftPanel, RightPanel}
import org.mikesajak.commander.util.Keys
import org.mikesajak.commander.util.Keys.Modifier.{Alt, Ctrl}
import scalafx.Includes._
import scalafx.scene.control.{SplitPane, TabPane}
import scalafx.scene.input.{KeyCode, KeyEvent}
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
                          operationMgr: OperationMgr) {
  leftDirPanelController.init(LeftPanel)
  rightDirPanelController.init(RightPanel)

  statusMgr.selectedPanel = LeftPanel

  mainPane.filterEvent(KeyEvent.KeyPressed) { ke: KeyEvent =>
    var handled = true
    if (Keys.hasNoModifiers(ke)) {
      ke.code match {
        case KeyCode.F3 => operationMgr.handleView()
        case KeyCode.F4 => operationMgr.handleEdit()
        case KeyCode.F5 => operationMgr.handleCopy()
        case KeyCode.F6 => operationMgr.handleMove()
        case KeyCode.F7 => operationMgr.handleMkDir()
        case KeyCode.F8 => operationMgr.handleDelete()
        case KeyCode.F10 => operationMgr.handleExit()
        // Debug: temporary
        case KeyCode.F2 => operationMgr.handleCountDirStats()

        case KeyCode.Tab => // todo

        case _ => handled = false
      }
    } else {
      ke.code match {
        case KeyCode.R if Keys.hasOnlyModifiers(ke, Ctrl) => operationMgr.handleRefreshAction()

        case KeyCode.F7 if Keys.hasOnlyModifiers(ke, Alt) =>operationMgr.handleFindAction()

        case _ => handled = false
      }
    }

    if (handled)
      ke.consume()
  }

}


