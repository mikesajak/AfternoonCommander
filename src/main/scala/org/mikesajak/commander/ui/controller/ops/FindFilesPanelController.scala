package org.mikesajak.commander.ui.controller.ops

import com.typesafe.scalalogging.Logger
import javafx.scene.{control => jfxctrl}
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
  def init(startDir: VDirectory, dialog: Dialog[ButtonType])
  def getSelectedResult: Option[VPath]
  def getAllResults: Seq[VPath]
}

object FindFilesPanelController {
  val GoToPattButtonType = new ButtonType("Go to path")
  val ShowAsListButtonType = new ButtonType("Show as list")
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

                                   resourceMgr: ResourceManager,
                                   fsMgr: FilesystemsManager)
    extends FindFilesPanelController {
  private val logger = Logger[FindFilesPanelControllerImpl]

  private var goToPathButton: Button = _
  private var showAsListButton: Button = _
  private var searchIsPending = false

  headerImageView.image = resourceMgr.getIcon("file-find.png", IconSize.Big)
  curPathLabel.text = null
  searchStatsLabel.text = null

  def init(startDir: VDirectory, dialog: Dialog[ButtonType]): Unit = {
    searchInTextField.text = startDir.absolutePath

    goToPathButton = dialog.dialogPane.value.lookupButton(FindFilesPanelController.GoToPattButtonType).asInstanceOf[jfxctrl.Button]
    showAsListButton = dialog.dialogPane.value.lookupButton(FindFilesPanelController.ShowAsListButtonType).asInstanceOf[jfxctrl.Button]
    goToPathButton.disable = true
    showAsListButton.disable = true

    searchResultsListView.cellFactory = { _ =>
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
        if (!searchIsPending) startSearch()
        else stopSearch()
    }

    filenameSearchTextCombo.onAction = _ => startSearchButton.fire()

    searchResultsListView.selectionModel.value.selectedIndex.onChange { goToPathButton.disable = searchResultsListView.selectionModel.value.getSelectedIndex < 0 }
    searchResultsListView.items.value.onChange { showAsListButton.disable = searchResultsListView.items.value.size == 0 }

    Platform.runLater { filenameSearchTextCombo.requestFocus() }
  }

  override def getAllResults: Seq[VPath] = List(searchResultsListView.items.value: _*)

  override def getSelectedResult: Option[VPath] =
    Option(searchResultsListView.selectionModel.value.getSelectedItem)

  private def prepareSearchService(searchService: Service[SearchProgress]): Unit = {
    searchService.onRunning = _ => searchStarted()
    searchService.onFailed = e => searchStopped(e.getSource.getValue.asInstanceOf[SearchProgress], cancelled = true)
    searchService.onSucceeded = e => { searchStopped(e.getSource.getValue.asInstanceOf[SearchProgress], cancelled = false);  }
    searchService.onCancelled = e => searchStopped(e.getSource.getValue.asInstanceOf[SearchProgress], cancelled = true)

    searchService.value.onChange { if (searchIsPending) updateProgress(searchService.value.value, finished = false, cancelled = false) }
  }

  private def searchStarted(): Unit = {
    searchIsPending = true
    updateButtons()
    progressImageView.image = resourceMgr.getIcon("loading-chasing-arrows.gif")
    progressImageView.visible = true
  }

  private def searchStopped(finalStatus: SearchProgress, cancelled: Boolean): Unit = {
    searchIsPending = false
    updateButtons()
    progressImageView.image = null
    progressImageView.visible = false
    updateProgress(finalStatus, finished = true, cancelled = cancelled)
  }

  private def updateButtons(): Unit = {
    startSearchButton.text = if (searchIsPending) "Stop" else "Start search"
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
