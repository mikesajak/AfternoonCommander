package org.mikesajak.commander.ui.controller

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
                          @nested[DirPanelController] rightDirPanelController: DirPanelControllerInterface) {
  leftDirPanelController.init("LeftPanel")
  rightDirPanelController.init("RightPanel")
}