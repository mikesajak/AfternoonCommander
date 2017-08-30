package org.mikesajak.commander.ui.controller

import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.ui.controller.PanelId.{LeftPanel, RightPanel}

import scalafx.scene.control.{SplitPane, TabPane}
import scalafxml.core.macros.{nested, sfxml}

/**
 * Created by mike on 25.10.14.
 */
@sfxml
class MainPanelController(dirsSplitPane: SplitPane,
                          leftTabPane: TabPane,
                          rightTabPane: TabPane,
                          @nested[DirPanelController] leftDirPanelController: DirPanelControllerInterface,
                          @nested[DirPanelController] rightDirPanelController: DirPanelControllerInterface,

                          statusMgr: StatusMgr) {
  leftDirPanelController.init(LeftPanel)
  rightDirPanelController.init(RightPanel)

  statusMgr.selectedPanel = LeftPanel
}