package org.mikesajak.commander.ui.controller

import javafx.scene.{Parent, control}

import com.google.inject.Key
import com.google.inject.name.Names
import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.config.Configuration
import org.mikesajak.commander.fs.{FS, FilesystemsManager, VDirectory}
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.ui.{ResourceManager, UILoader}
import org.mikesajak.commander.{ApplicationContext, ApplicationController, BookmarkMgr}

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

  def oppositePanel(panelId: PanelId): PanelId = panelId match {
    case LeftPanel => RightPanel
    case _ => LeftPanel
  }
}

trait DirPanelControllerIntf {
  def init(panelId: PanelId)
  def updateCurTab(path: VDirectory)
}
/**
  * Created by mike on 14.04.17.
  */
@sfxml
class DirPanelController(tabPane: TabPane,
                         favDirsButton: Button,
                         prevDirButton: Button,
                         topDirButton: Button,
                         homeDirButton: Button,
                         drivesCombo: ComboBox[String],
                         freeSpaceLabel: Label,
                         topUIPane: Pane,

                         config: Configuration,
                         fsMgr: FilesystemsManager,
                         statusMgr: StatusMgr,
                         bookmarkMgr: BookmarkMgr,
                         resourceManager: ResourceManager,
                         appController: ApplicationController)
    extends DirPanelControllerIntf {

  private val logger = Logger[DirPanelController]
  private var dirTabManager: DirTabManager = _

  def init(panelId: PanelId) {
    // TODO: better way of getting dependency - use injection!!
    dirTabManager = ApplicationContext.globalInjector.getInstance(Key.get(classOf[DirTabManager],
                                                                          Names.named(panelId.toString)))

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

    tabPane.tabs.clear()
    dirTabManager.clearTabs()

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
      dirTabManager.addTab(t, tab.controller)
      tabPane += tab
      tabPane.selectionModel.select(tab.text.value)
    }

    tabPane += createNewTabTab()

    val selectedPath = tabPaths.head

    tabPane.getSelectionModel.selectFirst()

    registerListeners(panelId)
  }

  def handleFavDirsButton(): Unit = {
    val addBookmarkItem = new MenuItem() {
      text = s"Add bookmark (current directory) -> ${dirTabManager.selectedTab.dir}"
      onAction = ae => bookmarkMgr.addBookmark(dirTabManager.selectedTab.dir)
    }
    val bookmarks = bookmarkMgr.bookmarks.map(b => new MenuItem() {
      text = b.toString
      onAction = ae => {
        val maybePath = fsMgr.resolvePath(b.toString).map(_.directory)
        maybePath.foreach(dir => setCurrentTabDir(dir))

        if (maybePath.isEmpty)
          logger.warn(s"Bookmark path is cannot be found/resolved. Skipping. $b")
      }
    })
    val ctxMenu = new ContextMenu() {
      items.add(addBookmarkItem)
      if (bookmarks.nonEmpty) {
        items.add(new SeparatorMenuItem())
        bookmarks.foreach(b => items.add(b))
      }
    }
    val favButtonBounds = favDirsButton.localToScreen(favDirsButton.boundsInLocal.value)
    ctxMenu.show(appController.mainStage, favButtonBounds.getMinX, favButtonBounds.getMaxY)
  }

  def handlePrevDirButton(): Unit = {
    dirTabManager.selectedTab.dir.parent
      .foreach(dir => setCurrentTabDir(dir))
  }

  def handleTopDirButton(): Unit = {
    val rootDir = FS.rootDirOf(dirTabManager.selectedTab.dir)
    setCurrentTabDir(rootDir)
  }

  def handleHomeDirButton(): Unit = {
    val homeDir = fsMgr.homeDir
    setCurrentTabDir(homeDir)
  }

  private def setCurrentTabDir(dir: VDirectory): Unit = {
    dirTabManager.selectedTab.controller.setCurrentDirectory(dir)
    updateCurTab(dir)
  }

  private def registerListeners(panelId: PanelId): Unit = {
    var tabSelectionPending = false
    tabPane.selectionModel().selectedIndexProperty().addListener { (ov, oldIdx, newIdx) =>
      println(s"$panelId - tab selection change $oldIdx->$newIdx")
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
      } else {
        // todo: do not reload synchronously, just schedule async reload (important when dir is remote and reloading will take some time)
        logger.debug(s"$panelId - reloading tab: $tabIdx: ${dirTabManager.tab(tabIdx).dir}")
        dirTabManager.tab(tabIdx).controller.reload()
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

      val lastDirTab = tabPane.tabs.size - 2
      tabPane.selectionModel.value.select(lastDirTab)
    }
  }

  private def createTab(path: VDirectory) = {
    new DirTab(this, path)
  }

  def updateCurTab(path: VDirectory): Unit = {
    val selectionModel = tabPane.selectionModel.value
    val curTab = selectionModel.getSelectedItem

    dirTabManager.tab(selectionModel.getSelectedIndex).dir = path
    DirTab.updateTab(curTab, path)
  }

  private def createNewTabTab() = {
    new Tab {
      closable = false
//      disable = true
      graphic = new ImageView(resourceManager.getIcon("plus-box-24.png"))
      text = ""
    }
  }
}

object DirTab {
  // this is a small hack - scalafx TabPane does not store actual tabs
  // but their delegates (original javafx tabs) so all data in scalafx Tabs is lost
  // as soon tab is added... This sucks...

  // This is helper to operate on tabs to work around that.

  def updateTab(jfxTab: control.Tab, path: VDirectory): Unit = {
    jfxTab.text = path.name
    jfxTab.tooltip = path.absolutePath
  }

}

class DirTab(panelController: DirPanelControllerIntf, tabPath: VDirectory) extends Tab {
  import DirTab._

  private val dirTableLayout = "/layout/file-tab-layout.fxml"

  val (root: Parent, controller) = UILoader.loadScene[DirTableControllerIntf](dirTableLayout)

  private val pane = new Node(root){}
  content = pane

  updateTab(this, tabPath)

  controller.init(panelController, tabPath)
}
