package org.mikesajak.commander.ui.controller

import com.google.common.eventbus.Subscribe
import org.mikesajak.commander._
import org.mikesajak.commander.fs.{FS, FilesystemsManager, VDirectory, VPath}
import org.mikesajak.commander.ui.controller.DirViewEvents.CurrentDirChangeNotification
import org.mikesajak.commander.ui.{IconSize, ResourceManager}
import org.mikesajak.commander.util.PathUtils
import scalafx.geometry.Side
import scalafx.scene.control.{Button, ContextMenu, MenuItem, SeparatorMenuItem}
import scalafx.scene.image.ImageView
import scalafxml.core.macros.sfxml

trait PanelActionsBarController {
  def init(listener: CurrentDirAware)
}

@sfxml
class PanelActionsBarControllerImpl(favDirsButton: Button,
                                    prevDirButton: Button,
                                    parentDirButton: Button,
                                    topDirButton: Button,
                                    homeDirButton: Button,

                                    panelId: PanelId,
                                    dirTabManager: DirTabManager,
                                    resourceMgr: ResourceManager,
                                    fsMgr: FilesystemsManager,
                                    bookmarkMgr: BookmarkMgr,
                                    historyMgr: HistoryMgr,
                                    eventBus: EventBus)
    extends PanelActionsBarController {

  private var curDirListeners = List[CurrentDirAware]()

  def init(listener: CurrentDirAware): Unit = {
    curDirListeners ::= listener
    updateButtons()

    eventBus.register(this)
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
        graphic = new ImageView(resourceMgr.getIcon("icons8-add-tag-48.png", IconSize.Tiny))
        onAction = _ => bookmarkMgr.addBookmark(selectedDir)
      }
    }

    def mkPathMenuItem(path: VPath): MenuItem = new MenuItem {
      text = path.toString
      onAction = _ => notifyDirectoryChange(path.directory)
    }

    val bookmarks = bookmarkMgr.bookmarks.map(mkPathMenuItem)

    val panelHistoryItems = historyMgr.panelHistoryCtrl(panelId).getAll.take(5)

    val globalHistoryItems = historyMgr.globalHistoryCtrl.getAll
                                       .filter(i => !historyMgr.panelHistoryCtrl(panelId).getAll.contains(i))
                                       .take(5)

    val ctxMenu = new ContextMenu() {
      items.add(titleMenuItem("file_group_panel.bookmarks_menu.title"))
      items.add(mkBookmarkMenuItem())
      if (bookmarks.nonEmpty) {
        items.add(new SeparatorMenuItem())
        bookmarks.foreach(b => items.add(b))
      }

      if (panelHistoryItems.nonEmpty) {
        items.add(new SeparatorMenuItem())
        items.add(titleMenuItem("file_group_panel.local_history_menu.title"))
        panelHistoryItems.map(mkPathMenuItem)
                         .foreach(i => items.add(i))
      }

      if (globalHistoryItems.nonEmpty) {
        items.add(new SeparatorMenuItem())
        items.add(titleMenuItem("file_group_panel.global_history_menu.title"))
        globalHistoryItems.map(mkPathMenuItem)
                          .foreach(i => items.add(i))
      }
    }

    ctxMenu.show(favDirsButton, Side.Bottom, 0, 0)
  }

  def handlePrevDirButton(): Unit = {
    historyMgr.panelHistoryCtrl(panelId).last
              .foreach(notifyDirectoryChange)
  }

  def handleParentDirButton(): Unit = {
    dirTabManager.selectedTab.dir.parent
                 .foreach(dir => notifyDirectoryChange(dir))
  }

  def handleTopDirButton(): Unit = {
    val rootDir = FS.rootDirOf(dirTabManager.selectedTab.dir)
    notifyDirectoryChange(rootDir)
  }

  def handleHomeDirButton(): Unit = {
    val homeDir = fsMgr.homeDir
    notifyDirectoryChange(homeDir)
  }

  private def updateButtons(): Unit = {
    prevDirButton.disable = historyMgr.panelHistoryCtrl(panelId).isEmpty
    parentDirButton.disable = dirTabManager.selectedTab.dir.parent.isEmpty
  }

  private def notifyDirectoryChange(directory: VDirectory): Unit = {
    curDirListeners.foreach(_.setDirectory(directory))
  }

  //noinspection UnstableApiUsage
  @Subscribe
  def updateCurTabUIAfterDirChange(event: CurrentDirChangeNotification): Unit = {
    if (event.panelId == panelId)
      updateButtons()
  }
}
