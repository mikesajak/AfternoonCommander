package org.mikesajak.commander.ui.controller.properties

import org.mikesajak.commander.fs.Permission.{READ_DATA, WRITE_DATA}
import org.mikesajak.commander.fs.{AccessPermissions, Permission, UnixAccessPermissions, VPath}
import scalafx.beans.property.StringProperty
import scalafx.scene.control.{TreeItem, TreeTableColumn, TreeTableView}
import scalafx.scene.layout.Pane
import scalafxml.core.macros.{nested, sfxml}

trait AccessPermissionsPropertiesPanelController {
  def init(path: VPath): Unit
}

case class PermsTableRow(who: String, permValue: String, perm: Set[Permission])

object PermissionOrdering extends Ordering[Permission] {
  override def compare(x: Permission, y: Permission): Int =
    x match {
      case READ_DATA => if (y == READ_DATA) 0 else -1
      case WRITE_DATA =>
        y match {
          case READ_DATA => 1
          case WRITE_DATA => 0
          case _ => -1
        }
      case _ => x.toString.compareTo(y.toString)
    }
}

object UserOrdering extends Ordering[(String, Set[Permission])] {
  override def compare(x: (String, Set[Permission]), y: (String, Set[Permission])): Int = x match {
    case (name, _) if name.startsWith("owner:") => if (y._1.startsWith("owner:")) 0 else -1
    case (name, _) if name.startsWith("group:") =>
      y match {
        case (yName, _) if yName.startsWith("owner:") => 1
        case (yName, _) if yName.startsWith("group:") => 0
        case _ => -1
      }
    case _ => 1
  }
}

@sfxml
class AccessPermissionsPropertiesPanelControllerImpl(unixAccessPanel: Pane,
                                                     @nested[UnixAccessPanelControllerImpl] unixAccessPanelController: UnixAccessPanelController,
                                                     windowsAccessPanel: Pane,
                                                     @nested[WindowsAccessPanelControllerImpl] windowsAccessPanelController: WindowsAccessPanelController,
                                                     permsTreeTableView: TreeTableView[PermsTableRow],
                                                     nameTreeTableColumn: TreeTableColumn[PermsTableRow, String],
                                                     permTreeTableColumn: TreeTableColumn[PermsTableRow, String])
    extends AccessPermissionsPropertiesPanelController {
  override def init(path: VPath): Unit = {
    nameTreeTableColumn.cellValueFactory = { node => StringProperty(Option(node.value.getValue).map(_.who).getOrElse("")) }
    permTreeTableColumn.cellValueFactory = { node => StringProperty(Option(node.value.getValue).map(_.permValue).getOrElse("")) }

    val permissions = path.permissions
    permsTreeTableView.root = new TreeItem[PermsTableRow](PermsTableRow("root", "", Set.empty)) {
      children = permissions.permissions.toSeq
                            .sorted(UserOrdering)
                            .map(p => mkTreeItem(p._1, p._2))
      expanded = true
    }

    permsTreeTableView.showRoot = false

    permissions match {
      case unixPermissions: UnixAccessPermissions =>
        windowsAccessPanel.visible = false
        unixAccessPanelController.init(path, unixPermissions)
      case accessPermissions: AccessPermissions =>
        unixAccessPanel.visible = false
        windowsAccessPanelController.init(path, accessPermissions)
    }
  }

  private def mkTreeItem(who: String, perms: Set[Permission]) =
    new TreeItem[PermsTableRow](PermsTableRow(who, "", perms)) {
      children = perms.toSeq
                      .sorted(PermissionOrdering)
                      .map(p => mkSinglePermTreeItem(who, p))
      expanded = true
    }

  private def mkSinglePermTreeItem(who: String, perm: Permission) =
    new TreeItem[PermsTableRow](PermsTableRow("", perm.toString, Set.empty)) {
      expanded = true
    }
}
