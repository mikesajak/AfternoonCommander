package org.mikesajak.commander.ui.controller.settings

import java.text.NumberFormat

import javafx.{scene => jfxs}
import org.mikesajak.commander.ApplicationController
import org.mikesajak.commander.config.Configuration
import org.mikesajak.commander.ui.ResourceManager
import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Node
import scalafx.scene.control.{Button, _}
import scalafx.scene.layout._
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
                                  categoryPanel: BorderPane,
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
      categoryPanel.center = newVal.value.value.panel
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

      maxWidth = Double.MaxValue
      maxHeight = Double.MaxValue

      columnConstraints = Seq(new ColumnConstraints(), new ColumnConstraints() {
        hgrow = Priority.Always
        fillWidth = true
        prefWidth = Region.USE_COMPUTED_SIZE
        maxWidth = Double.MaxValue
      })

      children = items.zipWithIndex.flatMap { case (item, index) =>
        val controls = item match {
          case boolItem: BoolSettingsItem => Seq(mkBoolEditor(boolItem))
          case intItem: IntSettingsItem => Seq(mkLabel(intItem), mkIntEditor(intItem))
          case colorItem: ColorSettingsItem => Seq(mkLabel(colorItem), mkColorEditor(colorItem))
          case execFileItem: ExecFileSettingsItem => Seq(mkLabel(execFileItem), mkFileEditor(execFileItem))
          case _ => Seq(mkLabel(item), mkStringEditor(item))
        }
        setupControlsPropertiesAndLayout(item, controls, index)
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

  private def setupControlsPropertiesAndLayout(item: SettingsItem, regions: Seq[Region], row: Int): Unit = {
    regions.foreach { region =>
      region.alignmentInParent = Pos.BaselineLeft

      if (region.isInstanceOf[Control]) region.asInstanceOf[Control].tooltip = new Tooltip(item.description)
    }

    if (regions.size == 1) GridPane.setConstraints(regions.head, 0, row, 2, 1)
    else regions.zipWithIndex.foreach { case (region, index) => GridPane.setConstraints(region, index, row) }
  }

  private def mkBoolEditor(item: BoolSettingsItem) =
    new CheckBox(item.name) {
      selected = item.value
    }

  private def mkLabel(item: SettingsItem) =
    new Label(item.name) {
      alignmentInParent = Pos.BaselineLeft
    }

  private def mkStringEditor(item: SettingsItem) =
    new TextField() {
      alignmentInParent = Pos.BaselineLeft
      text = item.value.toString
    }

  private def mkIntEditor(item: IntSettingsItem) =
    new TextField() {
      val converter = new FormatStringConverter[Number](NumberFormat.getIntegerInstance)
      textFormatter = new TextFormatter(converter)
      text = item.value.toString
    }

  private def mkColorEditor(item: ColorSettingsItem) = new ColorPicker(item.value)

  private def mkFileEditor(item: ExecFileSettingsItem) = {
    new HBox {
      spacing = 5
      hgrow = Priority.Always
      alignmentInParent = Pos.BaselineLeft
      alignment = Pos.BaselineLeft
      maxWidth = Double.MaxValue
      prefWidth = Region.USE_COMPUTED_SIZE

      private val textField = new TextField() {
        alignmentInParent = Pos.BaselineLeft
        alignment = Pos.BaselineLeft
        hgrow = Priority.Always
        maxWidth = Double.MaxValue
        prefWidth = Region.USE_COMPUTED_SIZE
        text = item.value.toString
        tooltip = new Tooltip(item.description)
      }

      private val fileChooserButton = new Button {
        text = "..."
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
