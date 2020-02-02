package org.mikesajak.commander.ui.controller.settings

import java.text.NumberFormat

import javafx.{scene => jfxs}
import org.mikesajak.commander.config.Configuration
import org.mikesajak.commander.ui.ResourceManager
import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Node
import scalafx.scene.control._
import scalafx.scene.layout.{AnchorPane, HBox, VBox}
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

  def createSettingsPanel(items: Seq[SettingsItem]): VBox = {
    new VBox(5) {
      children = items.map { item =>
        item.itemType match {
          case t if classOf[Boolean].isAssignableFrom(t) => new CheckBox(item.name) {
            selected = item.value.asInstanceOf[Boolean]
            tooltip = new Tooltip(item.description)
          }

          case t if classOf[Int].isAssignableFrom(t) => new HBox(5) {
            alignment = Pos.BaselineLeft
            children = Seq(
              new Label(item.name) {
                tooltip = new Tooltip(item.description)
              },
              new TextField() {
                val converter = new FormatStringConverter[Number](NumberFormat.getIntegerInstance)
                textFormatter = new TextFormatter(converter)
                text = item.value.asInstanceOf[Int].toString
                tooltip = new Tooltip(item.description)
              }
            )
          }

          case _ => new HBox(5) {
            alignment = Pos.BaselineLeft
            children = Seq(
              new Label(item.name) {
                tooltip = new Tooltip(item.description)
              },
              new TextField() {
                text = item.value.toString
                tooltip = new Tooltip(item.description)
              }
            )
          }
        }
      }
    }

  }
}
