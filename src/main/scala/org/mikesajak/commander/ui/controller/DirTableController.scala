package org.mikesajak.commander.ui.controller

import java.util.function.Predicate

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.config.{ConfigKey, ConfigObserver, Configuration}
import org.mikesajak.commander.fs._
import org.mikesajak.commander.handler.{ActionFileHandler, ContainerFileHandler, FileHandlerFactory}
import org.mikesajak.commander.ui.controller.DirViewEvents.{CurrentDirChange, NewTabRequest}
import org.mikesajak.commander.ui.{IconResolver, PropertiesCtrl, ResourceManager, UIUtils}
import org.mikesajak.commander.units.DataUnit
import org.mikesajak.commander.util.PathUtils
import org.mikesajak.commander.{ApplicationController, EventBus, FileTypeManager, HistoryMgr}
import scalafx.Includes._
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.collections.transformation.{FilteredBuffer, SortedBuffer}
import scalafx.geometry.{Insets, Side}
import scalafx.scene.control._
import scalafx.scene.input.{KeyCode, KeyEvent, MouseButton, MouseEvent}
import scalafx.stage.Popup
import scalafxml.core.macros.sfxml

import scala.collection.JavaConverters._
/**
  * Created by mike on 14.04.17.
  */
class FileRow(val path: VPath, resourceMgr: ResourceManager) {

  private val (pname, pext) = PathUtils.splitNameByExt(path.name)

  val name = new StringProperty(mkName(path))
  val extension = new StringProperty(mkExt(path))
  val size = new StringProperty(formatSize(path))
  val modifyDate = new StringProperty(path.modificationDate.toString)
  val attributes = new StringProperty(path.attributes.toString)

  private def mkName(p: VPath): String = if (p.isDirectory) s"[${p.name}]" else pname
  private def mkExt(p: VPath): String = if (p.isDirectory) "" else pext

  def formatSize(vFile: VPath): String =
    path match {
      case _: PathToParent => resourceMgr.getMessage("file_row.parent")
      case _: VDirectory => resourceMgr.getMessageWithArgs("file_row.num_elements",
                                                           Array(path.directory.children.size))
      case _: VFile => DataUnit.formatDataSize(path.size)
    }

  override def toString: String = s"FileRow(path=$path, $name, $extension, $size, $modifyDate, $attributes)"

  def canEqual(other: Any): Boolean = other.isInstanceOf[FileRow]

  override def equals(other: Any): Boolean = other match {
    case that: FileRow =>
      (that canEqual this) &&
        path == that.path
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(path)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

trait DirTableControllerIntf {
  def init(path: VDirectory)
  def dispose()
  def currentDirectory: VDirectory
  def setCurrentDirectory(path: VDirectory, focusedPath: Option[VPath] = None)
  def focusedPath: VPath
  def selectedPaths: Seq[VPath]
  def reload(): Unit
  def setTableFocusOn(pathOption: Option[VPath])
  def setTableFocusOn(pathName: String) // TODO: change to some matcher, or first occurrence, or startsWith etc.
  def historyMgr: HistoryMgr
}

@sfxml
class DirTableController(curDirField: TextField,
                         addTabButton: Button,
                         dirTableView: TableView[FileRow],
                         idColumn: TableColumn[FileRow, VPath],
                         nameColumn: TableColumn[FileRow, String],
                         extColumn: TableColumn[FileRow, String],
                         sizeColumn: TableColumn[FileRow, String],
                         dateColumn: TableColumn[FileRow, String],
                         attribsColumn: TableColumn[FileRow, String],
                         statusLabel1: Label,
                         statusLabel2: Label,

                         panelId: PanelId,
                         fileTypeMgr: FileTypeManager,
                         fileHandlerFactory: FileHandlerFactory,
                         resourceMgr: ResourceManager,
                         iconResolver: IconResolver,
                         config: Configuration,
                         eventBus: EventBus,
                         panelController: DirPanelControllerIntf,
                         appController: ApplicationController,
                         propertiesCtrl: PropertiesCtrl)
    extends DirTableControllerIntf {

  import org.mikesajak.commander.ui.UIParams._

  private val logger = Logger[DirTableController]

  private var curDir: VDirectory = _

  private val tableRows = ObservableBuffer[FileRow]()
  private val filteredRows = new FilteredBuffer(tableRows)
  private val sortedRows = new SortedBuffer(filteredRows)

  override val historyMgr = new HistoryMgr()

  addTabButton.padding = Insets.Empty
  curDirField.prefHeight <== addTabButton.height

  private val configObserver: ConfigObserver = new ConfigObserver {
    override val observedKey = ConfigKey("file_panel", "*")

    override def configChanged(key: ConfigKey): Unit = key match {
      case ConfigKey("file_panel", "show_hidden") =>
        filteredRows.predicate = createShowHiddenFilesPredicate()
      case _ =>
    }
  }

  override def focusedPath: VPath = dirTableView.selectionModel.value.getSelectedItem.path

  override def selectedPaths: ObservableBuffer[VPath] = dirTableView.selectionModel.value.getSelectedItems.map(_.path)

  override def init(path: VDirectory) {
    statusLabel1.styleClass += "file_panel_status"
    statusLabel2.styleClass += "file_panel_status"

    dirTableView.selectionModel.value.selectionMode = SelectionMode.Multiple

    registerCurDirFieldUpdater()

    dirTableView.selectionModel.value.selectedItems.onChange {
      val selectedPaths = dirTableView.selectionModel.value.selectedItems.map(_.path)
      updateSelectionBar(selectedPaths)
    }

    idColumn.cellValueFactory = { t => ObjectProperty(t.value.path) }
    idColumn.cellFactory = { _: TableColumn[FileRow, VPath] =>
      new TableCell[FileRow, VPath]() {
        item.onChange { (_, _, newValue) =>
          graphic = Option(newValue)
              .flatMap(cellPath => iconResolver.findIconFor(cellPath))
              .orNull
        }
      }
    }

    nameColumn.cellValueFactory = { _.value.name }
    nameColumn.cellFactory = { _: TableColumn[FileRow, String] =>
      new TableCell[FileRow, String] {
        item.onChange { (_, _, newValue) =>
          text = newValue
          val curRowIdx = index.value
          val tableItems = tableView.value.items.value
          val fileRowOpt = if (curRowIdx >= 0 && curRowIdx < tableItems.size) Option(tableItems.get(curRowIdx))
                           else None
          tooltip = fileRowOpt.map { fileRow =>
            val fileType = fileTypeMgr.detectFileType(fileRow.path)
            val fileTypeName = fileTypeMgr.descriptionOf(fileType)

            new Tooltip() {
              text = resourceMgr.getMessageWithArgs("file_table_panel.row_tooltip",
                List(fileRow.path.absolutePath, fileRow.modifyDate.value,
                  fileRow.attributes.value, fileRow.size.value, fileTypeName))
            }
          }.orNull
        }
      }
    }
    extColumn.cellValueFactory = { _.value.extension }
    sizeColumn.cellValueFactory = { _.value.size }
    dateColumn.cellValueFactory = { _.value.modifyDate }
    attribsColumn.cellValueFactory = { _.value.attributes }
    dirTableView.rowFactory = { _: TableView[FileRow] =>
      val row = new TableRow[FileRow]()

      row.handleEvent(MouseEvent.MouseClicked) { event: MouseEvent =>
        if (!row.isEmpty) {
          val rowPath = row.item.value.path
          event.button match {
            case MouseButton.Primary if event.clickCount == 2 =>
              handleAction(rowPath)
            case MouseButton.Secondary =>
              showContextMenu(row, event)
            case MouseButton.Middle =>
            case _ =>
          }
        }
      }
      row
    }

    dirTableView.handleEvent(KeyEvent.KeyPressed) { event: KeyEvent => handleKeyEvent(event) }

    eventBus.register(configObserver)
    filteredRows.predicate = createShowHiddenFilesPredicate()

    dirTableView.items = sortedRows

    addTabButton.onAction = _ => eventBus.publish(NewTabRequest(panelId, curDir))

    setCurrentDirectory(path)
  }

  override def dispose(): Unit = {
    eventBus.unregister(configObserver)
  }

  private def createShowHiddenFilesPredicate() = {
    row: FileRow =>
      val showHidden = config.boolProperty("file_panel", "show_hidden").getOrElse(false)
      row.path.isInstanceOf[PathToParent] ||
        !row.path.attributes.contains(Attrib.Hidden) || showHidden
  }

  override def currentDirectory: VDirectory = curDir

  override def setCurrentDirectory(directory: VDirectory, focusedPath: Option[VPath] = None): Unit = {
    if (curDir != null)
      historyMgr.add(resolveTargetDir(curDir))
    val prevDir = curDir
    val newDir = resolveTargetDir(directory)
    curDir = newDir
    curDirField.text = newDir.absolutePath
    initTable(newDir)

    setTableFocusOn(focusedPath)

    updateStatusBar(newDir)

    eventBus.publish(CurrentDirChange(panelId, Option(prevDir), newDir))
  }

  private def resolveTargetDir(directory: VDirectory) = {
    directory match {
      case p: PathToParent => p.targetDir
      case _ => directory
    }
  }

  private def updateStatusBar(directory: VDirectory): Unit =
    statusLabel1.text = prepareSummary(directory.childDirs, directory.childFiles)

  private def updateSelectionBar(selectedPaths: Seq[VPath]): Unit = {
    val (dirs, files) = selectedPaths.partition(_.isDirectory)
    statusLabel2.text = prepareSummary(dirs.map(_.asInstanceOf[VDirectory]), files.map(_.asInstanceOf[VFile]))
  }

  private def prepareSummary(dirs: Seq[VDirectory], files: Seq[VFile]): String = {
    val numDirs = dirs.size
    val totalSize = files.map(_.size).sum
    val sizeUnit = DataUnit.findDataSizeUnit(totalSize)
    resourceMgr.getMessageWithArgs("file_table_panel.status.message",
                                   Array(numDirs, files.size,
                                         sizeUnit.convert(totalSize),
                                         sizeUnit.symbol))
  }

  override def reload(): Unit = {
    setCurrentDirectory(curDir, Some(focusedPath))
  }

  override def setTableFocusOn(pathOption: Option[VPath]): Unit = {
    val selIndex = pathOption.map { path =>
      math.max(0, dirTableView.items.value.indexWhere(row => row.path.name == path.name))
    }.getOrElse(0)

    selectIndex(selIndex)
  }

  override def setTableFocusOn(pathName: String): Unit = {
    val selIndex =
      math.max(0, dirTableView.items.value.indexWhere(row => row.path.name == pathName))

    selectIndex(selIndex)
  }

  private def handleKeyEvent(event: KeyEvent) {
    logger.debug(s"handleKeyEvent: $event")
    event.code match {
      case KeyCode.Enter =>
        val items = dirTableView.selectionModel.value.selectedItems
        if (items.nonEmpty) {
          handleAction(items.head.path)
        }

      case KeyCode.S if event.shortcutDown => showFilterPopup()

      case _ =>
    }
  }

  private def handleAction(path: VPath, forceOSAction: Boolean = false): Unit = {
    val handler = if (forceOSAction) Some(fileHandlerFactory.getDefaultOSActionHandler(path))
                  else fileHandlerFactory.getFileHandler(path)

    handler.foreach {
      case containerHandler: ContainerFileHandler => changeDir(containerHandler.getContainerDir)
      case actionHandler: ActionFileHandler => actionHandler.handle()
      case _ => logger.debug(s"File handler couldn't be found for path $path.")
    }
  }

  private def changeDir(directory: VDirectory): Unit = {
    val selection = directory match {
      case _: PathToParent => Some(curDir)
      case _ => None
    }
    setCurrentDirectory(directory, selection)
  }

  private def showContextMenu(row: TableRow[FileRow], event: MouseEvent): Unit = {
    val rowPath = row.item.value.path

    val menuItems = Seq(
      new MenuItem() {
        text = resourceMgr.getMessage("file_table_panel.context_menu.run_on_action")
        graphic = null
        onAction = _ => handleAction(rowPath, forceOSAction = true)
      },
      new SeparatorMenuItem(),
      new MenuItem() {
        text = resourceMgr.getMessage("file_table_panel.context_menu.properties")
        graphic = null
        onAction = _ => propertiesCtrl.showPropertiesOf(rowPath)
      })

    val cm = new ContextMenu(menuItems: _*)
    cm.show(row, Side.Bottom, event.x, 0)
  }

  private def showFilterPopup(): Unit = {
    val curFilterPredicate: Predicate[_ >: FileRow] = filteredRows.predicate.value

    var itemToFocusOnExit = dirTableView.focusModel.value.focusedItem.value

    val textField: TextField = new TextField() {
      prefWidth = dirTableView.width.value
    }
    val popup: Popup = new Popup() {
      content += textField
      autoHide = true
      // TODO: disable configuration changes while popup is opened? is it necessary?
      onHiding = { _ =>
        filteredRows.predicate = createShowHiddenFilesPredicate()
        val selection =
          dirTableView.items.value.indexWhere(row => row.path.name == itemToFocusOnExit.path.name)
        focusIndex(selection)
      }
    }

    textField.text.onChange { (_,_, textValue) =>
      val filterNamePredicate: Predicate[_ >: FileRow] = r => r.path.name.toLowerCase.contains(textValue.toLowerCase)
      filteredRows.predicate = row => curFilterPredicate.test(row) && filterNamePredicate.test(row)
    }

    textField.handleEvent(KeyEvent.KeyPressed) { ke: KeyEvent =>
      ke.code match {
        case KeyCode.Escape =>
          popup.hide()
        case KeyCode.Up =>
          val sel = dirTableView.focusModel.value.focusedIndex.value - 1
          dirTableView.focusModel.value.focus(sel)
          ke.consume()
        case KeyCode.Down =>
          val sel = dirTableView.focusModel.value.focusedIndex.value + 1
          dirTableView.focusModel.value.focus(sel)
          ke.consume()
        case KeyCode.Enter =>
          if (dirTableView.focusModel.value.getFocusedIndex >= 0) {
            itemToFocusOnExit = dirTableView.focusModel.value.focusedItem.value
            popup.hide()
          }
        case _ =>
      }
    }

    val bounds = dirTableView.localToScreen(dirTableView.boundsInLocal.value)
    popup.show(appController.mainStage, bounds.minX, bounds.maxY)
  }

  private def initTable(directory: VDirectory) {
    val sortedChildDirs = directory.childDirs.sortBy(d => d.name)
    val dirs = if (directory.parent.isDefined) new PathToParent(directory) +: sortedChildDirs
               else sortedChildDirs
    val files = directory.childFiles.sortBy(f => f.name)

    val fileRows = (dirs ++ files).view
      .map(f => new FileRow(f, resourceMgr))
      .toList

    tableRows.setAll(fileRows.asJava)
  }

  private def selectIndex(index: Int): Unit = {
    val prevSelection = dirTableView.getSelectionModel.getSelectedIndex
    if (prevSelection != index) {
      dirTableView.getSelectionModel.select(math.max(index, 0))
      scrollTo(index)
    }
  }

  private def focusIndex(index: Int): Unit = {
    logger.debug(s"focusIndex($index)")
    val prevFocus = dirTableView.focusModel.value.getFocusedIndex
    if (prevFocus != index) {
      val i = math.max(index, 0)
      logger.debug(s"  index=$i")
      dirTableView.getSelectionModel.select(i)
      scrollTo(index)
    }
  }

  private def scrollTo(index: Int): Unit = {
    dirTableView.scrollTo(math.max(index - NumPrevVisibleItems, 0))
  }

  private def registerCurDirFieldUpdater(): Unit = {
    var pending = false
    curDirField.text.onChange { (_, _, _) =>
      try {
        if (!pending) {
          pending = true
          var textWidth = UIUtils.calcTextBounds(curDirField).getWidth
          val componentWidth = curDirField.getBoundsInLocal.getWidth
          while (textWidth > componentWidth) {
            curDirField.text = shortenDirText(curDirField.text.value)
            textWidth = UIUtils.calcTextBounds(curDirField).getWidth
            curDirField.insets // FIXME: ???
          }
        }
      } finally {
        pending = false
      }
    }

    curDirField.width.onChange { (_, _, _) =>
      curDirField.text = curDir.absolutePath // FIXME: ??? shortenDirText?
    }
  }

  private def shortenDirText(text: String): String = {
    // TODO: smarter shortening - e.g. cut the middle of the path, leave the beginning and ending etc. /root/directory/.../lastDir
    text.substring(1)
  }
}

object DirViewEvents {
  case class CurrentDirChange(panelId: PanelId, prevDir: Option[VDirectory], curDir: VDirectory)
  case class NewTabRequest(panelId: PanelId, curDir: VDirectory)
}