package org.mikesajak.commander.ui.controller.ops

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.fs.{FilesystemsManager, VDirectory, VPath}
import org.mikesajak.commander.task._
import org.mikesajak.commander.ui.{IconSize, ResourceManager}
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.collections.ObservableBuffer
import scalafx.concurrent.Service
import scalafx.scene.control._
import scalafx.scene.image.ImageView
import scalafxml.core.macros.sfxml

trait FindFilesPanelController {
  def init(startDir: VDirectory)
}

@sfxml
class FindFilesPanelControllerImpl(headerImageView: ImageView,
                                   filenameSearchTextCombo: ComboBox[String],
                                   searchInTextField: TextField,
                                   filenameCaseCheckbox: CheckBox,
                                   filenameRegexCheckbox: CheckBox,
                                   filenameInverseCheckbox: CheckBox,
                                   searchContentCheckbox: CheckBox,
                                   contentSearchTextCombo: ComboBox[String],
                                   contentCaseCheckbox: CheckBox,
                                   contentRegexCheckbox: CheckBox,
                                   contentInverseCheckbox: CheckBox,
                                   searchResultsValueLabel: Label,
                                   searchResultsListView: ListView[VPath],
                                   progressImageView: ImageView,
                                   curPathLabel: Label,
                                   searchStatsLabel: Label,
                                   startSearchButton: Button,
                                   goToPathButton: Button,
                                   showAsListButton: Button,

                                   resourceMgr: ResourceManager,
                                   fsMgr: FilesystemsManager)
    extends FindFilesPanelController {
  private val logger = Logger[FindFilesPanelControllerImpl]

  private var pending = false

  headerImageView.image = resourceMgr.getIcon("file-find.png", IconSize.Big)
  curPathLabel.text = null
  searchStatsLabel.text = null

  def init(startDir: VDirectory): Unit = {
    searchInTextField.text = startDir.absolutePath

    searchResultsListView.cellFactory = { p =>
      val cell = new ListCell[VPath]
      cell.item.onChange { (_,_,elem) => cell.text = if (elem != null) elem.absolutePath else null }
      cell
    }

    var searchService: Service[SearchProgress] = null
    startSearchButton.disable = false

    def startSearch(): Unit = {
      fsMgr.resolvePath(searchInTextField.text.value).foreach { startDir =>
        val criteria = mkSearchCriteria()
        logger.info(s"Starting search  in: $startDir, criteria=$criteria")
        searchService = new BackgroundService(new FindFilesTask(criteria, startDir.directory, resourceMgr))
        prepareSearchService(searchService)
        searchResultsListView.items.value.clear()
        searchService.start()
      }
      // TODO: error/info if directory is not valid
    }

    def stopSearch(): Unit = {
      searchService.cancel()
    }

    startSearchButton.onAction = { _ =>
        if (!pending) startSearch()
        else stopSearch()
    }

    filenameSearchTextCombo.onAction = _ => startSearchButton.fire()

    searchResultsListView.selectionModel.value.selectedIndex.onChange { goToPathButton.disable = searchResultsListView.selectionModel.value.getSelectedIndex < 0 }
    searchResultsListView.items.value.onChange { showAsListButton.disable = searchResultsListView.items.value.size == 0 }

    Platform.runLater { filenameSearchTextCombo.requestFocus() }
  }

  private def prepareSearchService(searchService: Service[SearchProgress]): Unit = {
    searchService.onRunning = _ => searchStarted()
    searchService.onFailed = e => searchStopped(e.getSource.getValue.asInstanceOf[SearchProgress], true)
    searchService.onSucceeded = e => { searchStopped(e.getSource.getValue.asInstanceOf[SearchProgress], false);  }
    searchService.onCancelled = e => searchStopped(e.getSource.getValue.asInstanceOf[SearchProgress], true)

    searchService.value.onChange { if (pending) updateProgress(searchService.value.value, false, false) }
  }

  private def searchStarted(): Unit = {
    pending = true
    updateButtons()
    progressImageView.image = resourceMgr.getIcon("loading-chasing-arrows.gif")
    progressImageView.visible = true
  }

  private def searchStopped(finalStatus: SearchProgress, cancelled: Boolean): Unit = {
    pending = false
    updateButtons()
    progressImageView.image = null
    progressImageView.visible = false
    updateProgress(finalStatus, true, cancelled)
  }

  private def updateButtons(): Unit = {
    startSearchButton.text = if (pending) "Stop" else "Start search"
  }

  private def updateProgress(searchProgress: SearchProgress, finished: Boolean, cancelled: Boolean): Unit = {
    searchResultsValueLabel.text = s"[${searchProgress.results.size} elements]"
    searchResultsListView.items = ObservableBuffer(searchProgress.results)

    if (cancelled) curPathLabel.text = s"Search cancelled. Found ${searchProgress.results.size} items."
    else if (finished) curPathLabel.text = s"Search finished. Found ${searchProgress.results.size} items."
    else curPathLabel.text = searchProgress.curPath.toString

    searchStatsLabel.text = s"Searched dirs=${searchProgress.dirStats.numDirs}, files=${searchProgress.dirStats.numFiles}" // TODO: i18
  }

  private def mkSearchCriteria() = {
    Search(SearchCriteria(filenameSearchTextCombo.value.value, filenameCaseCheckbox.selected.value,
                          filenameRegexCheckbox.selected.value),
           None)
  }

}
