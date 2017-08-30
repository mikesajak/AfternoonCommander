package org.mikesajak.commander.ui.controller

import org.mikesajak.commander.status.{OperationMgr, StatusMgr}
import org.mikesajak.commander.ui.controller.PanelId.{LeftPanel, RightPanel}

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
                          @nested[DirPanelController] leftDirPanelController: DirPanelControllerInterface,
                          @nested[DirPanelController] rightDirPanelController: DirPanelControllerInterface,

                          statusMgr: StatusMgr,
                          operationMgr: OperationMgr) {
  leftDirPanelController.init(LeftPanel)
  rightDirPanelController.init(RightPanel)

  statusMgr.selectedPanel = LeftPanel

  mainPane.setStyle("-fx-border-color: Red")

  mainPane.filterEvent(KeyEvent.KeyPressed) { (ke: KeyEvent) =>
    ke.code match {
      case KeyCode.F3  => operationMgr.handleView()
      case KeyCode.F4  => operationMgr.handleEdit()
      case KeyCode.F5  => operationMgr.handleCopy()
      case KeyCode.F6  => operationMgr.handleMove()
      case KeyCode.F7  => operationMgr.handleMkDir()
      case KeyCode.F8  => operationMgr.handleDelete()
      case KeyCode.F10 => operationMgr.handleExit()

      case KeyCode.Tab => // todo

      case _ => // do nothing
    }
  }
}