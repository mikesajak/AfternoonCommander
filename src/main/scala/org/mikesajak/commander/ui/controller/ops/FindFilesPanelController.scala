package org.mikesajak.commander.ui.controller.ops

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.fs.{FilesystemsManager, VDirectory, VPath}
import org.mikesajak.commander.task._
import org.mikesajak.commander.ui.UIUtils.dialogButton
import org.mikesajak.commander.ui.{IconSize, ResourceManager}
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.concurrent.Service
import scalafx.scene.control._
import scalafx.scene.image.ImageView
import scalafxml.core.macros.sfxml

import scala.collection.JavaConverters._

trait FindFilesPanelController {
  def init(startDir: VDirectory, dialog: Dialog[ButtonType])
  def getSelectedResult: Option[VPath]
  def getAllResults: Seq[VPath]

  def stopSearch(): Unit
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
  private var closeButton: Button = _

  private var searchIsPending = false
  private var searchService: Service[SearchProgress] = _

  headerImageView.image = resourceMgr.getIcon("file-find.png", IconSize.Big)
  curPathLabel.text = null
  searchStatsLabel.text = null

  def init(startDir: VDirectory, dialog: Dialog[ButtonType]): Unit = {
    searchInTextField.text = startDir.absolutePath

    dialog.title = "Find files..."
    dialog.dialogPane.value.buttonTypes = Seq(FindFilesPanelController.GoToPattButtonType,
                                              FindFilesPanelController.ShowAsListButtonType,
                                              ButtonType.Close)

    goToPathButton = dialogButton(dialog, FindFilesPanelController.GoToPattButtonType)
    showAsListButton = dialogButton(dialog, FindFilesPanelController.ShowAsListButtonType)
    closeButton = dialogButton(dialog, ButtonType.Close)

    goToPathButton.disable = true
    showAsListButton.disable = true

    searchResultsListView.cellFactory = { _ =>
      val cell = new ListCell[VPath]
      cell.item.onChange { (_,_,elem) => cell.text = if (elem != null) elem.absolutePath else null }
      cell
    }

    startSearchButton.disable = false

    startSearchButton.onAction = { _ =>
        if (!searchIsPending) startSearch()
        else stopSearch()
    }

    filenameSearchTextCombo.onAction = _ => startSearchButton.fire()

    searchResultsListView.selectionModel.value.selectedIndex.onChange {
      goToPathButton.disable = searchResultsListView.selectionModel.value.getSelectedIndex < 0
    }
    searchResultsListView.items.value.onChange {
      showAsListButton.disable = searchResultsListView.items.value.size == 0
    }

    Platform.runLater { filenameSearchTextCombo.requestFocus() }
  }

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
    val results = searchResultsListView.items.value
    val resultsSelectionModel = searchResultsListView.selectionModel.value
    if (results.size() != searchProgress.results.size) {
      searchResultsValueLabel.text = s"[${searchProgress.results.size} elements]"

      val prevSelection = resultsSelectionModel.getSelectedIndex
      results.clear()
      results.addAll(searchProgress.results.asJava)
      resultsSelectionModel.selectIndices(prevSelection)
    }

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
