package org.mikesajak.commander.ui.controller

import javafx.scene.Parent

import com.google.inject.name.Names
import com.google.inject.{AbstractModule, Key}
import net.codingwell.scalaguice.ScalaModule
import org.mikesajak.commander.ApplicationContext
import org.mikesajak.commander.config.Configuration
import org.mikesajak.commander.fs.{FilesystemsManager, VDirectory}
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.ui.{ResourceManager, UILoader}

import scalafx.Includes._
import scalafx.scene.Node
import scalafx.scene.control._
import scalafx.scene.image.ImageView
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout.Pane
import scalafxml.core.macros.sfxml

sealed trait PanelId
object PanelId {
  case object LeftPanel extends PanelId
  case object RightPanel extends PanelId
}

trait DirPanelControllerInterface {
  def init(panelId: PanelId)
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
                         topUIPane: Pane,

                         config: Configuration,
                         fsMgr: FilesystemsManager,
                         statusMgr: StatusMgr,
                         resourceManager: ResourceManager)
    extends DirPanelControllerInterface {

  private val dirTableLayout = "/layout/file-tab-layout.fxml"
  private var dirTabManager: DirTabManager = _
  private var currentTab: Tab = _

  def init(panelId: PanelId) {
    // TODO: better way of getting dependency - use injection!!
    dirTabManager = ApplicationContext.globalInjector.getInstance(Key.get(classOf[DirTabManager],
                                                                          Names.named(panelId.toString)))

    favDirsButton.disable = true
    backDirButton.disable = true
    topDirButton.disable = true

    topUIPane.setStyle("-fx-border-color: Transparent")

    statusMgr.addPanelSelectionListener { (oldPanelId, newPanelId) =>
      if (newPanelId == panelId)
        topUIPane.setStyle("-fx-border-color: Blue")
      else
        topUIPane.setStyle("-fx-border-color: Transparent")
    }

    topUIPane.filterEvent(MouseEvent.MousePressed) {
      (me: MouseEvent) => statusMgr.selectedPanel = panelId
    }

    showHiddenToggleButton.onAction = a => config.setProperty("filePanel", "showHiddenFiles",
                                                              showHiddenToggleButton.selected.value)

    tabPane.tabs.clear()
    dirTabManager.clearTabs()

    var tabSelectionPending = false
    tabPane.selectionModel().selectedIndexProperty().addListener { (ov, oldIdx, newIdx) =>
      val prevIdx = oldIdx.intValue
      val tabIdx = newIdx.intValue
      if (!tabSelectionPending && isNewTabButton(tabIdx)) {
        println(s"Last tab!!")
        try {
          tabSelectionPending = true
          addNewTabToPane()
        } finally {
          tabSelectionPending = false
        }
      }

      if (tabIdx < tabPane.tabs.size) {
        val selectedTab = tabPane.tabs.get(tabIdx)
        statusMgr.selectedTabManager.selectedTabIdx = tabIdx
      }
    }

    tabPane.selectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) => {
      if (newTab.getGraphic != null) {
//        val oldPath = oldTab.asInstanceOf[DirTab].tabPath
//        tabPane += createTab(oldPath)
      }
//      currentTab = newTab
    })

    val numTabs = config.intProperty("tabs", s"$panelId.numTabs").getOrElse(0)

    val tabPathNames =
      config.stringSeqProperty("tabs", s"$panelId.tabs")
        .getOrElse(List(fsMgr.homePath))

    val tabPaths = tabPathNames
      .flatMap(path => fsMgr.resolvePath(path))
      // todo log problem with path
      .map(vpath => vpath.directory)

    tabPaths.foreach { t =>
      val tab = createTab(t)
      tabPane += tab
      tabPane.selectionModel.select(tab.text.value)
      dirTabManager.addTab(t, tab.controller)
      tabPane
    }

    tabPane += createNewTabTab()

    val selectedPath = tabPaths.head
    // todo - selection
    curDirField.text = selectedPath.absolutePath
//    tabPane.selectionModel.select(selectedPath.name)
    tabPane.getSelectionModel.selectFirst()
  }

  private def pathForNewTab() = {
    val newTabPathName = fsMgr.homePath
    fsMgr.resolvePath(newTabPathName).map(_.directory)
  }

  private def isNewTabButton(tabIdx: Int) =
    tabIdx == tabPane.tabs.size - 1 && tabPane.tabs.size > 1

  private def addNewTabToPane(): Unit = {
    tabPane.tabs.remove(tabPane.tabs.size - 1)

    val newTabPath = pathForNewTab()

    for (dir <- newTabPath) {
      val tab = createTab(dir)
      tabPane += tab
      dirTabManager.addTab(dir, tab.controller)
      tabPane += createNewTabTab()
    }
  }

  private def createTab(path: VDirectory) = {
    new DirTab(path)
  }

  private def createNewTabTab() = {
    new Tab {
      closable = false
//      disable = true
      graphic = new ImageView(resourceManager.getIcon("ic_add_box_black_24dp_1x.png"))
      text = ""
    }
  }
}

class DirTab(val tabPath: VDirectory) extends Tab {
  private val dirTableLayout = "/layout/file-tab-layout.fxml"

  val (root: Parent, controller) =
    UILoader.loadScene[DirTableControllerIntf](dirTableLayout,
                                           new DirTableContext(DirTableParams(tabPath)))

  text = tabPath.name
  private val pane = new Node(root){}
  content = pane
  tooltip = tabPath.absolutePath
}

case class DirPanelParams(panelId: PanelId)

class DirTableContext(dirTableParams: DirTableParams) extends AbstractModule with ScalaModule {
  def configure(): Unit = {
    bind(classOf[DirTableParams]).toInstance(dirTableParams)
  }
}
