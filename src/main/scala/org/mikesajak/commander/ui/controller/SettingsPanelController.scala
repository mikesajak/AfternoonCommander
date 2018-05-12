package org.mikesajak.commander.ui.controller

import javafx.{scene => jfxs}
import org.mikesajak.commander.config.Configuration
import org.mikesajak.commander.ui.UILoader
import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.control._
import scalafx.scene.layout.AnchorPane
import scalafxml.core.macros.sfxml

case class SettingsCategory(name: String,
                       panelLayout: Option[String],
                       children: Seq[SettingsCategory]) {
  lazy val panel: Option[Node] = panelLayout.map(layout =>
    new Node(UILoader.loadScene2(s"/layout/settings/$layout")){})
}
object SettingsCategory {
  def apply(name: String, layoutFile: String, children: Seq[SettingsCategory]) =
    new SettingsCategory(name, Some(layoutFile), children)

  def apply(name: String, layoutFile: String) =
    new SettingsCategory(name, Some(layoutFile), Seq.empty)

  def apply(name: String, children: Seq[SettingsCategory]) =
    new SettingsCategory(name, None, children)
}

/**
  * Created by mike on 23.04.17.
  */
@sfxml
class SettingsPanelController(categoriesTreeView: TreeView[SettingsCategory],
                              categoryPanel: AnchorPane,
                              okButton: Button,
                              applyButton: Button,
                              cancelButton: Button,
                              config: Configuration) {

  categoryPanel.margin = Insets(10, 10, 10, 10)

  okButton.disable = true
  applyButton.disable = true
  cancelButton.disable = true


  val categories = ObservableBuffer(
    SettingsCategory("General", "general-settings.fxml"),
    SettingsCategory("Appearance", Seq(
      SettingsCategory("File table colors", Some("color-settings.fxml"), Seq.empty))
    )
  )

  categoriesTreeView.root = new TreeItem[SettingsCategory] {
    expanded = true
    children = ObservableBuffer(categories map toTreeItem)
  }

  categoriesTreeView.cellFactory = { t =>
    new jfxs.control.TreeCell[SettingsCategory]() {
      val self: TreeCell[SettingsCategory] = this

      override def updateItem(item: SettingsCategory, empty: Boolean): Unit = {
        super.updateItem(item, empty)
        self.graphic = null
        self.text = if (empty || item == null) null
                    else item.name
      }
    }
  }

  categoriesTreeView.selectionModel().selectionMode = SelectionMode.Single

  categoriesTreeView.selectionModel().selectedItem.onChange{ (observale, oldVal, newVal) =>
//    println(s"onChange: observable=$observale, oldVal=$oldVal, newVal=$newVal")
    categoryPanel.children.clear()
    newVal.value.value.panel.map { settingsPanel =>
      categoryPanel.children += settingsPanel
    }
  }

//  categoriesTreeView.selectionModel.value.prope

  def toTreeItem(sc: SettingsCategory): TreeItem[SettingsCategory] = {
    if (sc.children.isEmpty) new TreeItem(sc)
    else new TreeItem(sc) {
      children = sc.children map toTreeItem
    }
  }
}
