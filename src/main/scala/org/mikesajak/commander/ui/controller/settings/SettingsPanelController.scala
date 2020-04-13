package org.mikesajak.commander.ui.controller.settings

import java.text.NumberFormat

import com.typesafe.scalalogging.Logger
import javafx.{scene => jfxs}
import org.mikesajak.commander.ApplicationController
import org.mikesajak.commander.config.Configuration
import org.mikesajak.commander.ui.{ResourceManager, UIUtils}
import scalafx.Includes._
import scalafx.beans.binding.Bindings
import scalafx.beans.property.ObjectProperty
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Node
import scalafx.scene.control.{Button, _}
import scalafx.scene.layout._
import scalafx.scene.text.{Font, FontWeight}
import scalafx.stage.FileChooser
import scalafx.util.converter.FormatStringConverter
import scalafxml.core.macros.sfxml

trait SettingsPanelController {
  def init(dialog: Dialog[Any]): Unit
}

case class SettingsGroupPanel(settingsGroup: SettingsGroup, panel: Node)

@sfxml
class SettingsPanelControllerImpl(categoriesTreeView: TreeView[SettingsGroupPanel],
                                  categoryPanel: BorderPane,

                                  config: Configuration,
                                  resourceMgr: ResourceManager,
                                  appController: ApplicationController)
    extends SettingsPanelController {
  private val logger = Logger[SettingsPanelControllerImpl]

  private val itemModifiedStyle = "-fx-font-weight: bold;"

  private val changedItems = ObjectProperty(Map[SettingsItem, Any]())

  override def init(dialog: Dialog[Any]) {
    categoryPanel.margin = Insets(10, 10, 10, 10)

    dialog.getDialogPane.buttonTypes = Seq(ButtonType.OK, ButtonType.Apply, ButtonType.Cancel)

    val okButton = UIUtils.dialogButton(dialog, ButtonType.OK)
    val applyButton = UIUtils.dialogButton(dialog, ButtonType.Apply)

    applyButton.disable <== Bindings.createBooleanBinding(() => changedItems.value.isEmpty, changedItems)

    okButton.onAction = _ => applyConfigChanges()
    applyButton.filterEvent(ActionEvent.Action) { ae: ActionEvent =>
      ae.consume()
      applyConfigChanges()
    }

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

  def applyConfigChanges(): Unit = {
    println(changedItems)
    changedItems.value.foreach { case (item, value) =>
      logger.debug(s"Applying settings change: ${item.name} -> $value")
      item.updateConfigValue(value)
    }
    logger.debug(s"All changes applied - clearing changes list.")
    changedItems.value = Map()
    config.save()
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

      children = items.zipWithIndex.flatMap { case (item, index) => createUIControlsForSettingItem(item, index) }
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

  private def createUIControlsForSettingItem(item: SettingsItem, index: Int): Seq[Region] = {
    val controls = item match {
      case boolItem: BoolSettingsItem => Seq(mkBoolEditor(boolItem))
      case intItem: IntSettingsItem =>
        val label = mkLabel(intItem)
        Seq(label, mkIntEditor(intItem, label))
      case colorItem: ColorSettingsItem =>
        val label = mkLabel(colorItem)
        Seq(label, mkColorEditor(colorItem, label))
      case execFileItem: ExecFileSettingsItem =>
        val label = mkLabel(execFileItem)
        Seq(label, mkFileEditor(execFileItem, label))
      case _ =>
        val label = mkLabel(item)
        Seq(label, mkStringEditor(item, label))
    }
    setupControlsPropertiesAndLayout(item, controls, index)
    controls
  }

  private def setupControlsPropertiesAndLayout(item: SettingsItem, regions: Seq[Region], row: Int): Unit = {
    regions.foreach { region =>
      region.alignmentInParent = Pos.BaselineLeft

      region match {
        case control: Control => control.tooltip = new Tooltip(item.description)
        case _ =>
      }
    }

    if (regions.size == 1) GridPane.setConstraints(regions.head, 0, row, 2, 1)
    else regions.zipWithIndex.foreach { case (region, index) => GridPane.setConstraints(region, index, row) }
  }

  private def mkBoolEditor(item: BoolSettingsItem) =
    new CheckBox(item.name) {
      selected = item.getConfigValue

      this.onAction = { _ =>
        val updated = updateChangedItems(item, selected.value)
        style.value = if (updated) itemModifiedStyle else ""
      }
    }

  private def mkLabel(item: SettingsItem) =
    new Label(item.name) {
      alignmentInParent = Pos.BaselineLeft
    }

  private def mkStringEditor(item: SettingsItem, label: Label) =
    new TextField() {
      alignmentInParent = Pos.BaselineLeft
      text = item.getConfigValue.toString

      this.onAction = { _ =>
        val updated = updateChangedItems(item, text.value)
        label.style.value = if (updated) itemModifiedStyle else ""
      }
    }

  private def mkIntEditor(item: IntSettingsItem, label: Label) =
    new TextField() {
      val converter = new FormatStringConverter[Number](NumberFormat.getIntegerInstance)
      textFormatter = new TextFormatter(converter)
      text = item.getConfigValue.toString

      this.onAction = { _ =>
        val updated = updateChangedItems(item, text.value.toLong)
        label.style.value = if (updated) itemModifiedStyle else ""
      }
    }

  private def mkColorEditor(item: ColorSettingsItem, label: Label) = {
    val picker = new ColorPicker(item.getConfigValue)
    picker.onAction = { _ =>
      val updated = updateChangedItems(item, picker.value.value)
      label.style.value = if (updated) itemModifiedStyle else ""
    }
    picker
  }

  private def mkFileEditor(item: ExecFileSettingsItem, label: Label) = {
    new HBox {
      spacing = 1
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
        text = item.getConfigValue
        tooltip = new Tooltip(item.description)

        onAction = { _ =>
          val updated = updateChangedItems(item, text.value)
          label.style.value = if (updated) itemModifiedStyle else ""
        }
      }

      private val fileChooserButton = new Button {
        text = "..."
        margin = Insets(0,0,0,0)
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

  private def updateChangedItems(item: SettingsItem, value: Any): Boolean = {
    if (value == item.getConfigValue) {
      logger.debug(s"Settings: value ${item.name} restored to default")
      changedItems.value -= item
      false
    }
    else {
      logger.debug(s"Settings: new value ${item.name} = $value")
      changedItems.value += item -> value
      true
    }
  }

}
