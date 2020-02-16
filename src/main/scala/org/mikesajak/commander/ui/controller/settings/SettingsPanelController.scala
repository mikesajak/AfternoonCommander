package org.mikesajak.commander.ui.controller.settings

import java.text.NumberFormat

import javafx.{scene => jfxs}
import org.mikesajak.commander.ApplicationController
import org.mikesajak.commander.config.Configuration
import org.mikesajak.commander.ui.ResourceManager
import org.mikesajak.commander.ui.controller.settings.SettingsType.{BoolType, ColorType, ExecFileType, IntType}
import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Node
import scalafx.scene.control.{Button, _}
import scalafx.scene.layout._
import scalafx.scene.paint.Color
import scalafx.scene.text.{Font, FontWeight}
import scalafx.stage.FileChooser
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
                                  resourceMgr: ResourceManager,
                                  appController: ApplicationController) extends SettingsPanelController {
  def init() {
    categoryPanel.margin = Insets(10, 10, 10, 10)

    okButton.disable = true
    applyButton.disable = true
    cancelButton.disable = true

    val itemFactory = new SettingsItemFactory(config, resourceMgr)

    val settingsGroups = itemFactory.settingsGroups

    categoriesTreeView.root = new TreeItem[SettingsGroupPanel] {
      expanded = true
      children = ObservableBuffer(settingsGroups map (group => toTreeItem(group, Seq())))
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

  def toTreeItem(settingsGroup: SettingsGroup, parents: Seq[SettingsGroup]): TreeItem[SettingsGroupPanel] = {
    if (settingsGroup.childGroups.isEmpty)
      new TreeItem(SettingsGroupPanel(settingsGroup, createSettingsPanel(settingsGroup.items, parents :+ settingsGroup)))
    else
      new TreeItem(SettingsGroupPanel(settingsGroup, createSettingsPanel(settingsGroup.items, parents :+ settingsGroup))) {
        children = settingsGroup.childGroups map (group => toTreeItem(group, parents :+ settingsGroup :+ group))
      }
  }

  def createSettingsPanel(items: Seq[SettingsItem], groups: Seq[SettingsGroup]): Pane = {
    val propertiesPanel = new GridPane {
      hgap = 5
      vgap = 5

      children = items.zipWithIndex.flatMap { case (item, index) =>
        val controls = item.itemType match {
          case BoolType => Seq(mkBoolEditor(item, index))
          case IntType => Seq(mkLabel(item, index), mkIntEditor(item, index))
          case ColorType => Seq(mkLabel(item, index), mkColorEditor(item, index))
          case ExecFileType => Seq(mkLabel(item, index), mkFileEditor(item, index))
          case _ => Seq(mkLabel(item, index), mkStringEditor(item, index))
        }
        setupControlsProperties(item, controls, index)
        controls
      }
    }

    val titleLabel: Label = new Label {
      text = groups.map(_.name).reduce((acc, name) => s"$acc > $name")
      font = Font(this.font.value.getFamily, FontWeight.Bold, this.font.value.getSize + 1)
    }

    new VBox {
      spacing = 20
      children = Seq(titleLabel, propertiesPanel)
    }
  }

  private def setupControlsProperties(item: SettingsItem, regions: Seq[Region], row: Int): Unit = {
    regions.foreach { region =>
      region.alignmentInParent = Pos.BaselineLeft

      if (region.isInstanceOf[Control]) region.asInstanceOf[Control].tooltip = new Tooltip(item.description)
    }

    if (regions.size == 1) GridPane.setConstraints(regions.head, 0, row, 2, 1)
    else regions.zipWithIndex.foreach { case (region, index) => GridPane.setConstraints(region, index, row) }
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
    }

  private def mkIntEditor(item: SettingsItem, row: Int) =
    new TextField() {
      val converter = new FormatStringConverter[Number](NumberFormat.getIntegerInstance)
      textFormatter = new TextFormatter(converter)
      text = item.value.asInstanceOf[Int].toString
    }

  private def mkColorEditor(item: SettingsItem, row: Int) =
    new ColorPicker(item.value.asInstanceOf[Color])

  private def mkFileEditor(item: SettingsItem, row: Int) = {
    new HBox {
      spacing = 5
      private val textField = new TextField() {
        alignmentInParent = Pos.BaselineLeft
        text = item.value.toString
        tooltip = new Tooltip(item.description)
      }

      private val fileChooserButton = new Button {
        text = "..."
        //graphic =
        alignmentInParent = Pos.BaselineLeft
        tooltip = new Tooltip(item.description)
        onAction = { _ =>
          val fileChooser = new FileChooser() {
            title = "Select editor application"
          }
          Option(fileChooser.showOpenDialog(appController.mainStage))
              .foreach { file => textField.text = file.getAbsolutePath }
        }
      }

      children = Seq(textField, fileChooserButton)
    }
  }

}
