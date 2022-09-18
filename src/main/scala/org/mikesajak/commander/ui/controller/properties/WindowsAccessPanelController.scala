package org.mikesajak.commander.ui.controller.properties

import org.mikesajak.commander.fs.{AccessPermissions, VPath}
import scalafx.scene.control.Label
import scalafxml.core.macros.sfxml

trait WindowsAccessPanelController {
  def init(path: VPath, accessPermissions: AccessPermissions): Unit
}

@sfxml
class WindowsAccessPanelControllerImpl(ownerNameLabel: Label)
    extends WindowsAccessPanelController {
  override def init(path: VPath, accessPermissions: AccessPermissions): Unit = {
    ownerNameLabel.text = accessPermissions.owner
  }
}
