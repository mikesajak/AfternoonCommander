package org.mikesajak.commander.ui.controller

import javax.swing.filechooser.FileSystemView

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import org.mikesajak.commander.config.Configuration
import org.mikesajak.commander.fs.local.LocalFS
import org.mikesajak.commander.fs.{FilesystemsManager, VDirectory}
import org.mikesajak.commander.ui.{ResourceManager, UILoader}

import scalafx.scene.Node
import scalafx.scene.control._
import scalafx.scene.image.ImageView
import scalafxml.core.macros.sfxml

sealed trait PanelId
object PanelId {
  case object LeftPanel extends PanelId
  case object RightPanel extends PanelId
}


trait DirPanelControllerInterface {
  def init(panelId: String)
}
/**
  * Created by mike on 14.04.17.
  */
@sfxml
class DirPanelController(tabPane: TabPane,
                         curDirField: TextField,
                         favDirsButton: Button,
                         backDirButton: Button,
                         topDirButton: Button,
                         drivesCombo: ComboBox[String],
                         freeSpaceLabel: Label,
                         showHiddenToggleButton: ToggleButton,
                         config: Configuration,
                         fsMgr: FilesystemsManager,
                         resourceManager: ResourceManager)
    extends DirPanelControllerInterface {

  private val dirTableLayout = "/layout/file-tab-layout.fxml"
  private var currentTab: Tab = _

  def init(panelId: String) {
    favDirsButton.disable = true
    backDirButton.disable = true
    topDirButton.disable = true

    showHiddenToggleButton.onAction = a => config.setProperty("filePanel", "showHiddenFiles", showHiddenToggleButton.selected.value)

    tabPane.tabs.clear()

    tabPane.selectionModel().selectedIndexProperty().addListener { (ov, oldIdx, newIdx) =>
      println(s"$panelId selected tab index change: $oldIdx->$newIdx")
      val selectedTab = tabPane.tabs.get(newIdx.intValue())
      println(s"selectedTab=$selectedTab")

    }

    tabPane.selectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) => {
      if (newTab.getGraphic != null) {
//        val oldPath = oldTab.asInstanceOf[DirTab].tabPath
//        tabPane += createTab(oldPath)
      }
//      currentTab = newTab
    })

    val numTabs = config.intProperty("tabs", s"${panelId}.numTabs").getOrElse(0)

    val tabPathNames =
      if (numTabs != 0) {
        for (i <- 0 until numTabs) yield {
          val tabPath = config.stringProperty("tabs", s"panelId.tab[$i].path").get
          tabPath
        }
      } else {
        val fsv = FileSystemView.getFileSystemView
        val path = LocalFS.mkLocalPathName(fsv.getHomeDirectory.getAbsolutePath)
        List(path)
      }


    val tabPaths = tabPathNames
      .flatMap(path => fsMgr.resolvePath(path))
      // todo log problem with path
      .map(vpath => vpath.directory)

    tabPaths
      .map(createTab)
      .foreach(t => tabPane += t)

    tabPane += createNewTabTab()

    val selectedPath = tabPaths.head
    // todo - selection
    curDirField.text = selectedPath.absolutePath
    tabPane.selectionModel.select(selectedPath.name)
    tabPane.getSelectionModel.selectFirst()
  }

  private def createTab(path: VDirectory) = {
    new DirTab(path)
  }

  private def createNewTabTab() = {
    new Tab {
      closable = false
      disable = true
      graphic = new ImageView(resourceManager.getIcon("ic_add_box_black_24dp_1x.png"))
      text = ""
    }
  }
}

class DirTab(val tabPath: VDirectory) extends Tab {
  private val dirTableLayout = "/layout/file-tab-layout.fxml"

  val root = UILoader.loadScene(dirTableLayout,
                                new DirTableContext(DirTableParams(tabPath)))

  text = tabPath.name
  val pane = new Node(root){}
  content = pane
}

case class DirPanelParams(panelId: PanelId)

class DirTableContext(dirTableParams: DirTableParams) extends AbstractModule with ScalaModule {
  def configure() = {
    bind(classOf[DirTableParams]).toInstance(dirTableParams)
  }
}
