package org.mikesajak.commander.ui.controller

import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import com.google.common.eventbus.Subscribe
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.inject.name.Names
import com.google.inject.{Binder, Key, Module}
import com.typesafe.scalalogging.Logger
import javafx.scene.{Parent, control => jfxctrl}
import org.mikesajak.commander._
import org.mikesajak.commander.config.Configuration
import org.mikesajak.commander.fs._
import org.mikesajak.commander.status.StatusChangeEvents.PanelSelected
import org.mikesajak.commander.status.StatusMgr
import org.mikesajak.commander.ui.controller.DirViewEvents.{CurrentDirChange, NewTabRequest}
import org.mikesajak.commander.ui.{IconResolver, IconSize, ResourceManager, UILoader}
import org.mikesajak.commander.units.DataUnit
import org.mikesajak.commander.util.Implicits._
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.geometry.Side
import scalafx.scene.Node
import scalafx.scene.control._
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

trait BreadCrumbItem
case class PathCrumbItem(path: VPath) extends BreadCrumbItem
case class PrevCrumbItems(prevPaths: Seq[VPath]) extends BreadCrumbItem

trait DirPanelControllerIntf extends CurrentDirAware {
  def init(panelId: PanelId)
  def requestFocus()
}
/**
  * Created by mike on 14.04.17.
  */
//noinspection UnstableApiUsage
@sfxml
class DirPanelController(tabPane: TabPane,
                         driveSelectionButton: Button,
                         freeSpaceLabel: Label,
                         freeSpaceBar: ProgressBar,
                         topUIPane: Pane,
                         newTabButton: Button,

                         config: Configuration,
                         fsMgr: FilesystemsManager,
                         statusMgr: StatusMgr,
                         bookmarkMgr: BookmarkMgr,
                         iconResolver: IconResolver,
                         resourceMgr: ResourceManager,
                         appController: ApplicationController,
                         eventBus: EventBus)
    extends DirPanelControllerIntf {

  private val logger = Logger[DirPanelController]
  private var panelId: PanelId = _
  private var dirTabManager: DirTabManager = _

  override def init(panelId: PanelId) {
    this.panelId = panelId

    dirTabManager = ApplicationContext.globalInjector.getInstance(Key.get(classOf[DirTabManager],
                                                                          Names.named(panelId.toString)))

    tabPane.tabs.clear()
    dirTabManager.clearTabs()

    val tabPathNames = config.stringSeqProperty("tabs", s"$panelId.tabs").getOrElse(List(fsMgr.homePath))

    val tabPaths = tabPathNames
      .flatMap(path => fsMgr.resolvePath(path)
                            .runIfEmpty(logger.info(s"Cannot resolve path tab path $path")))
      .map(_.directory)

    tabPaths.foreach { t =>
      val tab = createTab(t)
      dirTabManager.addTab(t, tab.controller)
      tabPane += tab
      tab.init()
      tabPane.selectionModel.select(tab.text.value)
    }

    val selectedPath = tabPaths.head
    updateDriveSelection(selectedPath)

    newTabButton.onAction =  _ => addNewTab(Some(dirTabManager.selectedTab.controller.currentDirectory))

    tabPane.getSelectionModel.selectFirst()

    registerUIListeners()

    registerEventBusSubscribers()

    startPeriodicTasks()
  }

  private def registerUIListeners(): Unit = {
    topUIPane.filterEvent(MouseEvent.MousePressed) { _: MouseEvent => selectThisPanel() }

    tabPane.selectionModel.value.selectedIndexProperty.onChange { (_, _, newIdx) =>
      val tabIdx = newIdx.intValue
      if (tabIdx < tabPane.tabs.size) {
        statusMgr.selectedTabManager.selectedTabIdx = tabIdx
      }
      // todo: do not reload synchronously, just schedule async reload (important when dir is remote and reloading will take some time)
      logger.debug(s"$panelId - reloading tab: $tabIdx: ${dirTabManager.tab(tabIdx).dir}")
      dirTabManager.tab(tabIdx).controller.reload()
    }
  }

  private def selectThisPanel(): Unit = {
    statusMgr.selectedPanel = panelId
  }

  private def registerEventBusSubscribers(): Unit = {
    eventBus.register(dirTabManager)

    topUIPane.setStyle("-fx-border-color: Transparent")
    eventBus.register(new AnyRef() {
      @Subscribe def handle(event: PanelSelected): Unit = {
        val style = if (event.newPanelId == panelId) "-fx-border-color: Blue" else "-fx-border-color: Transparent"
        topUIPane.setStyle(style)
      }
    })

    eventBus.register(this)
  }

  def requestFocus(): Unit = {
    dirTabManager.selectedTab.controller.requestFocus()
    selectThisPanel()
  }

  def handleDriveSelectionButton(): Unit = {
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
        graphic = iconResolver.findIconFor(fs, IconSize.Small)//, 24)))

        onAction = _ => setDirectory(fs.rootDirectory)
      })

    val ctxMenu = new ContextMenu(fsItems: _*)
    ctxMenu.show(driveSelectionButton, Side.Bottom, 0, 0)
  }

  override def setDirectory(directory: VDirectory): Unit = {
    if (directory.exists)
      dirTabManager.selectedTab.controller.setCurrentDirectory(directory)
    else
      logger.info(s"Cannot set current tab directory $directory. The target directory does not exist.")
  }

  @Subscribe
  def addNewTab(request: NewTabRequest): Unit =
    if (request.panelId == panelId)
      addNewTab(Some(request.curDir))

  private def addNewTab(newTabDir: Option[VDirectory]): Unit = {
    fsMgr.resolvePath(newTabDir.getOrElse(fsMgr.homeDir).toString)
      .map(_.directory)
      .foreach { dir =>
        val tab = createTab(dir)
        tabPane += tab
        dirTabManager.addTab(dir, tab.controller)
        tab.init()
        tabPane.selectionModel.value.select(tab)
      }
  }

  private def createTab(path: VDirectory) = new DirTab(panelId, path, dirTabManager)

  @Subscribe
  def updateCurTabUIAfterDirChange(event: CurrentDirChange): Unit = {
    if (event.panelId == panelId)
      updateCurTabUIAfterDirChange(event.curDir)
  }

  private def updateCurTabUIAfterDirChange(dir: VDirectory) {
    val curTab = tabPane.tabs(tabPane.selectionModel.value.getSelectedIndex)
    DirTab.updateTab(curTab, dir)

    updateDriveSelection(dir)
  }

  private def updateDriveSelection(dir: VDirectory): Unit = {
    val fs = fsMgr.findFilesystemFor(dir)
    driveSelectionButton.text = s"${fs.rootDirectory.absolutePath}"
    driveSelectionButton.graphic = iconResolver.findIconFor(fs, IconSize.Small)

    updateFreeSpace()
  }

  val scheduler = new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder().setDaemon(true).build())
  private def startPeriodicTasks(): Unit = {
    val task: Runnable = { () =>
        // FIXME: disabled until reload performance is fixed/improved
//      logger.debug(s"$panelId - reloading tab: ${dirTabManager.selectedTab.dir}")
//      Platform.runLater {
//        dirTabManager.selectedTab.controller.reload()
//      }
//      logger.trace(s"$panelId - finished reloading tab: ${dirTabManager.selectedTab.dir}")
      updateFreeSpace()
    }
    scheduler.scheduleAtFixedRate(task, 1, 5, TimeUnit.SECONDS)
  }

  private def updateFreeSpace(): Unit = {
    val curFs = fsMgr.findFilesystemFor(dirTabManager.selectedTab.dir)
    val free = curFs.freeSpace
    val total = curFs.totalSpace
    val freeUnit = DataUnit.findDataSizeUnit(free)
    val totalUnit = DataUnit.findDataSizeUnit(total)

    Platform.runLater {
      freeSpaceLabel.text = resourceMgr.getMessageWithArgs("file_group_panel.free_space.message",
        Array(freeUnit.convert(free), freeUnit.symbol, totalUnit.convert(total), totalUnit.symbol))
      freeSpaceBar.progress = (total - free).toDouble / total
    }
  }
}

object DirTab {
  // this is a small hack - scalafx TabPane does not store actual tabs
  // but their delegates (original javafx tabs) so all data in scalafx Tabs is lost
  // as soon tab is added... This sucks...

  // This is helper to operate on tabs to work around that.

  def updateTab(jfxTab: jfxctrl.Tab, path: VDirectory): Unit = {
    jfxTab.text = path.name
    jfxTab.tooltip = path.absolutePath
  }

}

class DirTab(panelId: PanelId, tabPath: VDirectory, dirTabManager: DirTabManager) extends Tab {
  import DirTab._

  private val dirTableLayout = "/layout/file-tab-layout.fxml"

  private val context = new Module {
    override def configure(binder: Binder): Unit = {
//      binder.bind(classOf[DirPanelControllerIntf]).toInstance(panelController)
      binder.bind(classOf[PanelId]).toInstance(panelId)
      binder.bind(classOf[DirTabManager]).toInstance(dirTabManager)
    }
  }

  val (root: Parent, controller) = UILoader.loadScene[DirTableController](dirTableLayout, context)

  private val pane = new Node(root){}
  content = pane

  updateTab(this, tabPath)

  this.onClosed = { _ => controller.dispose() }

  def init(): Unit = {
    controller.init(tabPath)
  }
}
