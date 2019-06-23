package org.mikesajak.commander.ui.controller

import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import com.google.common.eventbus.Subscribe
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.inject.name.Names
import com.google.inject.{Binder, Key, Module}
import com.typesafe.scalalogging.Logger
import javafx.scene.{Parent, control}
import org.mikesajak.commander._
import org.mikesajak.commander.config.Configuration
import org.mikesajak.commander.fs._
import org.mikesajak.commander.status.StatusChangeEvents.PanelSelected
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.ui.UIUtils._
import org.mikesajak.commander.ui.controller.DirViewEvents.{CurrentDirChange, NewTabRequest}
import org.mikesajak.commander.ui.{FSUIHelper, IconSize, ResourceManager, UILoader}
import org.mikesajak.commander.util.{DataUnit, PathUtils}
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.geometry.Insets
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
  def addNewTab(dir: Option[VDirectory] = None)
}
/**
  * Created by mike on 14.04.17.
  */
@sfxml
class DirPanelController(tabPane: TabPane,
                         favDirsButton: Button,
                         prevDirButton: Button,
                         parentDirButton: Button,
                         topDirButton: Button,
                         homeDirButton: Button,
                         driveSelectionButton: Button,
                         freeSpaceLabel: Label,
                         topUIPane: Pane,

                         config: Configuration,
                         fsMgr: FilesystemsManager,
                         statusMgr: StatusMgr,
                         bookmarkMgr: BookmarkMgr,
                         globalHistoryMgr: HistoryMgr,
                         resourceMgr: ResourceManager,
                         appController: ApplicationController,
                         eventBus: EventBus)
    extends DirPanelControllerIntf {

  private val logger = Logger[DirPanelController]
  private var panelId: PanelId = _
  private var dirTabManager: DirTabManager = _

  private val localHistoryMgr = new HistoryMgr

  override def init(panelId: PanelId) {
    this.panelId = panelId
    // TODO: better way of getting dependency - use injection!!
    dirTabManager = ApplicationContext.globalInjector.getInstance(Key.get(classOf[DirTabManager],
                                                                          Names.named(panelId.toString)))

    topUIPane.setStyle("-fx-border-color: Transparent")
    eventBus.register(new AnyRef() {
      @Subscribe def handle(event: PanelSelected) = {
        val style = if (event.newPanelId == panelId) "-fx-border-color: Blue" else "-fx-border-color: Transparent"
        topUIPane.setStyle(style)
      }
    })

    topUIPane.filterEvent(MouseEvent.MousePressed) {
      (me: MouseEvent) => statusMgr.selectedPanel = panelId
    }

    tabPane.tabs.clear()
    dirTabManager.clearTabs()

    val numTabs = config.intProperty("tabs", s"$panelId.numTabs").getOrElse(0)

    val tabPathNames = config.stringSeqProperty("tabs", s"$panelId.tabs").getOrElse(List(fsMgr.homePath))

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

    val selectedPath = tabPaths.head

    updateDriveSelection(selectedPath)

    tabPane.getSelectionModel.selectFirst()

    registerListeners()

    startPeriodicTasks()

    prevDirButton.disable = true

    eventBus.register(this)
  }

  def handleDriveSelectionButton(): Unit = {
    logger.warn(s"Drive selection not yet implemented1!!")

    val fsItems =
      fsMgr.discoverFilesystems().map(fs => new MenuItem() {
        text = s"${fs.rootDirectory.absolutePath} " +
          List(fs.attributes.get("info"),
               fs.attributes.get("drive"),
               fs.attributes.get("label"),
               fs.attributes.get("type").map(t => s"[$t]"))
            .flatten
            .reduce((a,b) => s"$a, $b") +
          s" [${DataUnit.mkDataSize(fs.freeSpace)} / ${DataUnit.mkDataSize(fs.totalSpace)}]"
        graphic = new ImageView(resourceMgr.getIcon(FSUIHelper.findIconFor(fs), IconSize.Small))//, 24)))

        onAction = ae => setCurrentTabDir(fs.rootDirectory)
      })

    val ctxMenu = new ContextMenu(fsItems: _*)
    showButtonCtxMenu(driveSelectionButton, ctxMenu, appController.mainStage)
  }

  def handleFavDirsButton(): Unit = {

    def titleMenuItem(messageKey: String): MenuItem =
      new MenuItem {
        text = resourceMgr.getMessage(messageKey)
        disable = true
      }

    def mkBookmarkMenuItem(): MenuItem = {
      val selectedDir = dirTabManager.selectedTab.dir
      val selectedDirRep = if (fsMgr.isLocal(selectedDir)) selectedDir.absolutePath
      else selectedDir.toString
      val selectedDirText = PathUtils.shortenPathTo(selectedDirRep, 50)
      new MenuItem() {
        text = resourceMgr.getMessageWithArgs("file_group_panel.add_bookmark_action.message", Array(selectedDirText))
        onAction = ae => bookmarkMgr.addBookmark(selectedDir)
      }
    }

    def mkPathMenuItem(path: VPath): MenuItem = new MenuItem {
      text = path.toString
      onAction = ae => setCurrentTabDir(path.directory)
    }

    val bookmarks = bookmarkMgr.bookmarks.map(mkPathMenuItem)

    val localHistoryItems = localHistoryMgr.getAll
      .take(5)
      .map(mkPathMenuItem)

    val globalHistoryItems = globalHistoryMgr.getAll
      .filter(i => !localHistoryMgr.getAll.contains(i))
      .take(5)
      .map(mkPathMenuItem)

    val ctxMenu = new ContextMenu() {
      items.add(titleMenuItem("file_group_panel.bookmarks_menu.title"))
      items.add(mkBookmarkMenuItem())
      if (bookmarks.nonEmpty) {
        items.add(new SeparatorMenuItem())
        bookmarks.foreach(b => items.add(b))
      }

      if (localHistoryItems.nonEmpty) {
        items.add(new SeparatorMenuItem())
        items.add(titleMenuItem("file_group_panel.local_history_menu.title"))
        localHistoryItems.foreach(i => items.add(i))
      }

      if (globalHistoryItems.nonEmpty) {
        items.add(new SeparatorMenuItem())
        items.add(titleMenuItem("file_group_panel.global_history_menu.title"))
        globalHistoryItems.foreach(i => items.add(i))
      }
    }

    showButtonCtxMenu(favDirsButton, ctxMenu, appController.mainStage)
  }

  def handlePrevDirButton(): Unit = {
    logger.warn("Prev dir button action not yet implemented")
    // TODO: implement
  }

  def handleParentDirButton(): Unit = {
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
    if (dir.fileSystem.exists(dir)) {
//      dirTabManager.selectedTab.controller.setCurrentDirectory(dir)
      updateCurTab(dir)
    } else {
      logger.info(s"Cannot set current tab directory $dir. The target directory does not exist.")
    }
  }

  @Subscribe
  def addNewTab(request: NewTabRequest): Unit =
    if (request.panelId == panelId)
      addNewTab(Some(request.curDir))

  override def addNewTab(newTabDir: Option[VDirectory]): Unit = {
    fsMgr.resolvePath(newTabDir.getOrElse(fsMgr.homeDir).toString)
      .map(_.directory)
      .foreach { dir =>
        val tab = createTab(dir)
        tabPane += tab
        dirTabManager.addTab(dir, tab.controller)
        tabPane.selectionModel.value.select(tab)
      }
  }

  private def registerListeners(): Unit = {
    tabPane.selectionModel().selectedIndexProperty().addListener { (ov, oldIdx, newIdx) =>
      val tabIdx = newIdx.intValue
      // todo: do not reload synchronously, just schedule async reload (important when dir is remote and reloading will take some time)
      logger.debug(s"$panelId - reloading tab: $tabIdx: ${dirTabManager.tab(tabIdx).dir}")
      dirTabManager.tab(tabIdx).controller.reload()

      if (tabIdx < tabPane.tabs.size) {
        statusMgr.selectedTabManager.selectedTabIdx = tabIdx
      }
    }
  }

  private def createTab(path: VDirectory) = {
    new DirTab(panelId, path)
  }

  @Subscribe
  def updateCurTab(event: CurrentDirChange): Unit =
    if (event.panelId == panelId)
      updateCurTab(event.curDir)

  override def updateCurTab(dir: VDirectory): Unit = {
    val selectionModel = tabPane.selectionModel.value
    val curTab = selectionModel.getSelectedItem

    // TODO: Hide this inside DirTabManager!
    dirTabManager.selectedTab.controller.setCurrentDirectory(dir)
    dirTabManager.tab(selectionModel.getSelectedIndex).dir = dir
    DirTab.updateTab(curTab, dir)

    updateDriveSelection(dir)

    // TODO: #eventbus Use publish event, and let any subscribers (global, local history managers etc.) listen and react
    localHistoryMgr.add(dir)
    globalHistoryMgr.add(dir)
  }

  private def updateDriveSelection(dir: VDirectory): Unit = {
    val matchingFss = fsMgr.discoverFilesystems().filter(fs => PathUtils.findParent(dir, fs.rootDirectory).isDefined)
    val fs =
      (matchingFss foldLeft dir.fileSystem)((a,b) => if (a.rootDirectory.segments.size > b.rootDirectory.segments.size) a else b)
    driveSelectionButton.text = s"${fs.rootDirectory.absolutePath}"
    driveSelectionButton.graphic = new ImageView(resourceMgr.getIcon(FSUIHelper.findIconFor(fs), IconSize.Small))
  }

  private def createNewTabTab() = {
    new Tab {
      closable = false
//      disable = true
      val button = new Button()
      button.graphic = new ImageView(resourceMgr.getIcon("plus-box.png", IconSize.Small))
      button.padding = Insets.Empty
      graphic = button

      text = null
    }
  }

  val scheduler = new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder().setDaemon(true).build())
  private def startPeriodicTasks(): Unit = {
    val task: Runnable = { () =>
        // FIXME: disabled until reload performance is fixed/improved
//      logger.debug(s"$panelId - reloading tab: ${dirTabManager.selectedTab.dir}")
//      dirTabManager.selectedTab.controller.reload()
      updateFreeSpace()
    }
    val f = scheduler.scheduleAtFixedRate(task, 1, 5, TimeUnit.SECONDS)

  }

  private def updateFreeSpace(): Unit = {
    val curFs = dirTabManager.selectedTab.dir.fileSystem
    val free = curFs.freeSpace
    val total = curFs.totalSpace
    val freeUnit = DataUnit.findDataSizeUnit(free)
    val totalUnit = DataUnit.findDataSizeUnit(total)

    Platform.runLater {
      freeSpaceLabel.text = resourceMgr.getMessageWithArgs("file_group_panel.free_space.message",
        Array(freeUnit.convert(free), freeUnit.symbol, totalUnit.convert(total), totalUnit.symbol))
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

class DirTab(panelId: PanelId, tabPath: VDirectory) extends Tab {
  import DirTab._

  private val dirTableLayout = "/layout/file-tab-layout.fxml"

  private val context = new Module {
    override def configure(binder: Binder): Unit = {
//      binder.bind(classOf[DirPanelControllerIntf]).toInstance(panelController)
      binder.bind(classOf[PanelId]).toInstance(panelId)
    }
  }

  val (root: Parent, controller) = UILoader.loadScene[DirTableControllerIntf](dirTableLayout, context)

  private val pane = new Node(root){}
  content = pane

  updateTab(this, tabPath)

  controller.init(tabPath)

  this.onClosed = { _ => controller.dispose() }
}
