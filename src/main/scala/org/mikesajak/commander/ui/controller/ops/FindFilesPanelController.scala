package org.mikesajak.commander.ui.controller.ops

import javafx.concurrent.Worker.State
import org.mikesajak.commander.fs.{FilesystemsManager, VDirectory, VPath}
import org.mikesajak.commander.task._
import org.mikesajak.commander.ui.UIUtils.dialogButton
import org.mikesajak.commander.ui.{IconSize, ResourceManager}
import org.mikesajak.commander.util.Throttler
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.concurrent.Service
import scalafx.scene.control._
import scalafx.scene.image.ImageView
import scalafx.scene.input.{KeyCode, KeyEvent, MouseButton}
import scalafxml.core.macros.sfxml
import scribe.Logging

import scala.jdk.CollectionConverters._

trait FindFilesPanelController {
  def init(startDir: VDirectory, dialog: Dialog[ButtonType]): (ButtonType, ButtonType, ButtonType)
  def getSelectedResult: Option[VPath]
  def getAllResults: Seq[VPath]

  def stopSearch(): Unit
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
                                   searchStatusLabel: Label,
                                   searchResultsLabel: Label,
                                   searchStatisticsLabel: Label,
                                   searchResultsListView: ListView[VPath],
                                   startSearchButton: Button,

                                   resourceMgr: ResourceManager,
                                   fsMgr: FilesystemsManager)
    extends FindFilesPanelController with Logging {

  private var goToPathButton: Button = _
  private var showAsListButton: Button = _
  private var closeButton: Button = _

  private var searchIsPending = false
  private var searchService: Service[SearchProgress] = _

  headerImageView.image = resourceMgr.getIcon("file-find.png", IconSize.Big)
  searchStatusLabel.text = null
  searchResultsLabel.text = null
  searchStatisticsLabel.text = null

  override def init(startDir: VDirectory, dialog: Dialog[ButtonType]): (ButtonType, ButtonType, ButtonType) = {
    searchInTextField.text = startDir.absolutePath

    val dialogPane: scalafx.scene.control.DialogPane = dialog.dialogPane.value

    dialog.title = resourceMgr.getMessage("find_files_dialog.title")

    val goToPathButtonType = new ButtonType(resourceMgr.getMessage("find_files_dialog.go_to_path_button"))
    val showAsListButtonType = new ButtonType(resourceMgr.getMessage("find_files_dialog.show_as_list_button"))
    val closeButtonType = new ButtonType(resourceMgr.getMessage("general.close_button"))

    dialogPane.buttonTypes = Seq(goToPathButtonType, showAsListButtonType, closeButtonType)

    goToPathButton = dialogButton(dialog, goToPathButtonType)
    showAsListButton = dialogButton(dialog, showAsListButtonType)
    closeButton = dialogButton(dialog, closeButtonType)

    goToPathButton.disable = true
    showAsListButton.disable = true

    searchResultsListView.cellFactory = { _ =>
      val cell = new ListCell[VPath]
      cell.item.onChange { (_,_,elem) => cell.text = if (elem != null) elem.absolutePath else null }
      cell
    }

    startSearchButton.disable = true

    startSearchButton.onAction = { _ =>
      if (!searchIsPending) startSearch()
      else stopSearch()
    }

    filenameSearchTextCombo.editor.value.text.onChange { (_, _, searchText) =>
      if (!searchIsPending) {
        startSearchButton.disable = searchText.isBlank
      }
    }

    dialogPane.filterEvent(KeyEvent.KeyPressed) { ke: KeyEvent =>
      if (ke.code == KeyCode.Escape)
        closeButton.fire()
    }

    searchResultsListView.selectionModel.value.selectedIndex.onChange {
      goToPathButton.disable = searchResultsListView.selectionModel.value.getSelectedIndex < 0
    }
    searchResultsListView.items.value.onChange {
      showAsListButton.disable = searchResultsListView.items.value.size == 0
    }

    searchResultsListView.onMouseClicked = { me =>
      if (me.button == MouseButton.Primary && me.clickCount == 2) {
        if (searchResultsListView.selectionModel.value.selectedIndex.value >= 0)
          goToPathButton.fire()
      }
    }

    Platform.runLater { filenameSearchTextCombo.requestFocus() }

    (goToPathButtonType, showAsListButtonType, closeButtonType)
  }

  def startSearch(): Unit = {
    fsMgr.resolvePath(searchInTextField.text.value).foreach { startDir =>
      stopSearch()

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
    if (searchService != null) {
      searchService.cancel()
      searchService = null
    }
  }

  override def getAllResults: Seq[VPath] = searchResultsListView.items.value.toList

  override def getSelectedResult: Option[VPath] =
    Option(searchResultsListView.selectionModel.value.getSelectedItem)

  def doUpdate(searchProgress: SearchProgress) {
    if (searchIsPending) Platform.runLater {
      updateProgress(searchProgress, finished = false, cancelled = false)
    }
  }

  private def prepareSearchService(searchService: Service[SearchProgress]): Unit = {
    searchService.state.onChange { (_, _, state) => state match {
      case State.RUNNING =>   searchStarted()
      case State.FAILED =>    searchStopped(searchService.value.value, cancelled = true)
      case State.SUCCEEDED => searchStopped(searchService.value.value, cancelled = false)
      case State.CANCELLED => searchStopped(searchService.value.value, cancelled = true)
      case _ =>
    }}

    val throttler = new Throttler[SearchProgress](50, doUpdate)
    Throttler.registerCancelOnServiceFinish(searchService, throttler)
    searchService.value.onChange { (_, _, value) => throttler.update(value) }
  }

  private def searchStarted(): Unit = {
    searchIsPending = true
    updateButtons()
    searchStatusLabel.graphic = new ImageView(resourceMgr.getIcon("loading-chasing-arrows.gif", IconSize(14)))
    searchStatusLabel.text = resourceMgr.getMessage("find_files_dialog.status.in_progress")
  }

  private def searchStopped(finalStatus: SearchProgress, cancelled: Boolean): Unit = {
    searchIsPending = false
    updateButtons()
    searchStatusLabel.graphic = null
    searchStatusLabel.text = if (cancelled) resourceMgr.getMessage("find_files_dialog.status.aborted")
                             else resourceMgr.getMessage("find_files_dialog.status.success")
    updateProgress(finalStatus, finished = true, cancelled = cancelled)
  }

  private def updateButtons(): Unit = {
    startSearchButton.text = if (searchIsPending) resourceMgr.getMessage("find_files_dialog.action.stop")
                             else resourceMgr.getMessage("find_files_dialog.action.start")
  }

  private def updateProgress(searchProgress: SearchProgress, finished: Boolean, cancelled: Boolean): Unit = {
    val results = searchResultsListView.items.value
    val resultsSelectionModel = searchResultsListView.selectionModel.value
    if (results.size() != searchProgress.results.size) {
      val prevSelection = resultsSelectionModel.getSelectedIndex
      results.clear()
      results.addAll(searchProgress.results.asJava)
      resultsSelectionModel.selectIndices(prevSelection)
    }

    searchResultsLabel.text = resourceMgr.getMessageWithArgs("find_files_dialog.result.message",
                                                             Seq(searchProgress.results.size))

    searchStatisticsLabel.text = resourceMgr.getMessageWithArgs("find_files_dialog.statistics.message",
                                                                Seq(searchProgress.dirStats.numDirs, searchProgress.dirStats.numFiles))
  }

  private def mkSearchCriteria() = {
    val searchText = filenameSearchTextCombo.editor.value.text.value
    Search(SearchCriteria(searchText,
                          filenameCaseCheckbox.selected.value,
                          filenameRegexCheckbox.selected.value,
                          filenameInverseCheckbox.selected.value),
           None)
  }

}
