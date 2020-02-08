package org.mikesajak.commander.ui.controller.settings

import java.text.NumberFormat

import javafx.{scene => jfxs}
import org.mikesajak.commander.config.Configuration
import org.mikesajak.commander.ui.ResourceManager
import org.mikesajak.commander.ui.controller.settings.SettingsType.{BoolType, ColorType, IntType}
import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Node
import scalafx.scene.control._
import scalafx.scene.layout.{AnchorPane, GridPane, Pane}
import scalafx.scene.paint.Color
import scalafx.util.converter.FormatStringConverter
import scalafxml.core.macros.sfxml

trait SettingsPanelController {
  def init(): Unit
}

case class SettingsGroupPanel(settingsGroup: SettingsGroup, panel: Node)

@sfxml
class SettingsPanelControllerImpl(categoriesTreeView: TreeView[SettingsGroupPanel],
                                  categoryPanel: AnchorPane,
                                  okButton: Button,
                                  applyButton: Button,
                                  cancelButton: Button,
                                  config: Configuration,
                                  resourceMgr: ResourceManager) extends SettingsPanelController {
  def init() {
    categoryPanel.margin = Insets(10, 10, 10, 10)

    okButton.disable = true
    applyButton.disable = true
    cancelButton.disable = true

    val itemFactory = new SettingsItemFactory(config, resourceMgr)

    val settingsGroups = itemFactory.settingsGroups

    categoriesTreeView.root = new TreeItem[SettingsGroupPanel] {
      expanded = true
      children = ObservableBuffer(settingsGroups map toTreeItem)
    }

    categoriesTreeView.cellFactory = { _ =>
      new jfxs.control.TreeCell[SettingsGroupPanel]() {
        val self: TreeCell[SettingsGroupPanel] = this

        override def updateItem(settingsGroupPanel: SettingsGroupPanel, empty: Boolean): Unit = {
          super.updateItem(settingsGroupPanel, empty)
          self.graphic = null
          self.text = if (empty || settingsGroupPanel == null) null
                      else settingsGroupPanel.settingsGroup.name
        }
      }
    }

    categoriesTreeView.selectionModel().selectionMode = SelectionMode.Single

    categoriesTreeView.selectionModel().selectedItem.onChange{ (_, _, newVal) =>
      categoryPanel.children.clear()
      categoryPanel.children = Seq(newVal.value.value.panel)
    }
  }

  def toTreeItem(settingsGroup: SettingsGroup): TreeItem[SettingsGroupPanel] = {
    if (settingsGroup.childGroups.isEmpty)
      new TreeItem(SettingsGroupPanel(settingsGroup, createSettingsPanel(settingsGroup.items)))
    else new TreeItem(SettingsGroupPanel(settingsGroup, createSettingsPanel(settingsGroup.items))) {
      children = settingsGroup.childGroups map toTreeItem
    }
  }

  def createSettingsPanel(items: Seq[SettingsItem]): Pane =
    new GridPane {
      hgap = 5
      vgap = 5

      children = items.zipWithIndex.flatMap { case (item, index) =>
        val controls = item.itemType match {
          case BoolType => Seq(mkBoolEditor(item, index))
          case IntType => Seq(mkLabel(item, index), mkIntEditor(item, index))
          case ColorType => Seq(mkLabel(item, index), mkColorEditor(item, index))
          case _ => Seq(mkLabel(item, index), mkStringEditor(item, index))
        }
        controls.foreach(control => setCommonProperties(item, control))
        controls
      }
    }

  private def setCommonProperties(item: SettingsItem, control: Control) = {
    control.alignmentInParent = Pos.BaselineLeft
    control.tooltip = new Tooltip(item.description)
    control
  }

  private def mkBoolEditor(item: SettingsItem, row: Int) =
    new CheckBox(item.name) {
      selected = item.value.asInstanceOf[Boolean]
      GridPane.setConstraints(this, 0, row, 2, 1)
    }

  private def mkLabel(item: SettingsItem, row: Int) =
    new Label(item.name) {
      alignmentInParent = Pos.BaselineLeft
      tooltip = new Tooltip(item.description)
      GridPane.setConstraints(this, 0, row)
    }

  private def mkStringEditor(item: SettingsItem, row: Int) =
    new TextField() {
      alignmentInParent = Pos.BaselineLeft
      text = item.value.toString
      tooltip = new Tooltip(item.description)

      GridPane.setConstraints(this, 1, row)
    }

  private def mkIntEditor(item: SettingsItem, row: Int) =
    new TextField() {
      val converter = new FormatStringConverter[Number](NumberFormat.getIntegerInstance)
      textFormatter = new TextFormatter(converter)
      text = item.value.asInstanceOf[Int].toString

      GridPane.setConstraints(this, 1, row)
    }

  private def mkColorEditor(item: SettingsItem, row: Int) =
    new ColorPicker(item.value.asInstanceOf[Color]) {
      GridPane.setConstraints(this, 1, row)
    }

}
