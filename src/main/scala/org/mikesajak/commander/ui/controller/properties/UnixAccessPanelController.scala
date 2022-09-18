package org.mikesajak.commander.ui.controller.properties

import org.mikesajak.commander.fs.Permission.{EXECUTE, READ_DATA, WRITE_DATA}
import org.mikesajak.commander.fs.{Permission, UnixAccessPermissions, VPath}
import org.mikesajak.commander.ui.ResourceManager
import scalafx.scene.control.Label
import scalafxml.core.macros.sfxml

trait UnixAccessPanelController {
  def init(path: VPath, unixAccessPermissions: UnixAccessPermissions): Unit
}

@sfxml
class UnixAccessPanelControllerImpl(ownerNameLabel: Label,
                                    ownerAccessLabel: Label,
                                    ownerReadAccessLabel: Label,
                                    ownerWriteAccessLabel: Label,
                                    ownerExecuteAccessLabel: Label,

                                    groupNameLabel: Label,
                                    groupAccessLabel: Label,
                                    groupReadAccessLabel: Label,
                                    groupWriteAccessLabel: Label,
                                    groupExecuteAccessLabel: Label,

                                    othersAccessLabel: Label,
                                    othersReadAccessLabel: Label,
                                    othersWriteAccessLabel: Label,
                                    othersExecuteAccessLabel: Label,

                                    resourceManager: ResourceManager) extends UnixAccessPanelController {

  override def init(path: VPath, unixAccessPermissions: UnixAccessPermissions): Unit = {
    ownerNameLabel.text = unixAccessPermissions.owner
    prepLabels(unixAccessPermissions.ownerPermissions, ownerAccessLabel, ownerReadAccessLabel, ownerWriteAccessLabel, ownerExecuteAccessLabel)

    groupNameLabel.text = unixAccessPermissions.group
    prepLabels(unixAccessPermissions.groupPermissions, groupAccessLabel, groupReadAccessLabel, groupWriteAccessLabel, groupExecuteAccessLabel)

    prepLabels(unixAccessPermissions.othersPermissions, othersAccessLabel, othersReadAccessLabel, othersWriteAccessLabel, othersExecuteAccessLabel)
  }

  private def prepLabels(permmissions: Set[Permission], accessLabel: Label, readLabel: Label, writeLabel: Label, execLabel: Label): Unit = {
    val read = permmissions.contains(READ_DATA)
    val write = permmissions.contains(WRITE_DATA)
    val exec = permmissions.contains(EXECUTE)

    accessLabel.text = (if (read) "r" else "") + (if (write) "w" else "") + (if (exec) "x" else "")
    readLabel.text = mkPermissionString(read)
    writeLabel.text = mkPermissionString(write)
    execLabel.text = mkPermissionString(exec)
  }


  private def mkPermissionString(present: Boolean) =
    resourceManager.getMessageWithArgs("properties_panel.access_rights_tab.unix_panel.access_msg", Seq(present.toNum))

  private implicit class BooleanToNumber(value: Boolean) {
    def toNum: Int = if (value) 1 else 0
  }

}
