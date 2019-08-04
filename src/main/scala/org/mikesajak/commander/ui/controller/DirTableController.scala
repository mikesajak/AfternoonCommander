package org.mikesajak.commander.ui.controller

import java.io.IOException
import java.util.function.Predicate

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.config.{ConfigKey, ConfigObserver, Configuration}
import org.mikesajak.commander.fs._
import org.mikesajak.commander.ui.controller.DirViewEvents.{CurrentDirChange, NewTabRequest}
import org.mikesajak.commander.ui.{ResourceManager, UIUtils}
import org.mikesajak.commander.util.TextUtil._
import org.mikesajak.commander.util.{DataUnit, PathUtils}
import org.mikesajak.commander.{ApplicationController, EventBus, FileTypeManager, HistoryMgr}
import scalafx.Includes._
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.collections.transformation.{FilteredBuffer, SortedBuffer}
import scalafx.geometry.Insets
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
                         resourceMgr: ResourceManager,
                         fileIconResolver: FileIconResolver,
                         config: Configuration,
                         eventBus: EventBus,
                         panelController: DirPanelControllerIntf,
                         appController: ApplicationController)
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
    idColumn.cellFactory = { tc: TableColumn[FileRow, VPath] =>
      new TableCell[FileRow, VPath]() {
        item.onChange { (_, _, newValue) =>
          graphic = Option(newValue)
              .flatMap(cellPath => fileIconResolver.findIconFor(cellPath))
              .orNull
        }
      }
    }

    nameColumn.cellValueFactory = { _.value.name }
    nameColumn.cellFactory = { tc: TableColumn[FileRow, String] =>
      new TableCell[FileRow, String] {
        item.onChange { (_, _, newValue) =>
          text = newValue
          val curRowIdx = index.value
          val tableItems = tableView.value.items.value
          val fileRowOpt = if (curRowIdx >= 0 && curRowIdx < tableItems.size) Option(tableItems.get(curRowIdx))
                           else None
          tooltip = fileRowOpt.map { fileRow =>
            val fileType = fileTypeMgr.detectFileType(fileRow.path)
            val fileTypeName = resourceMgr.getMessageOpt(s"file_type_manager.${camelToSnake(fileType.toString)}")
              .getOrElse(fileType.toString)

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
    dirTableView.rowFactory = { tableView =>
      val row = new TableRow[FileRow]()

      row.handleEvent(MouseEvent.MouseClicked) { event: MouseEvent =>
        if (!row.isEmpty) {
          val rowPath = row.item.value.path
          event.button match {
            case MouseButton.Primary if event.clickCount == 2 =>
              handleAction(rowPath)
            case MouseButton.Secondary =>
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

  override def setCurrentDirectory(directory: VDirectory, focusedPath: Option[VPath] = None): Unit = {
    if (curDir != null)
      historyMgr.add(curDir)
    val prevDir = curDir
    val newDir = directory match {
      case p: PathToParent => p.targetDir
      case _ => directory
    }
    curDir = newDir
    curDirField.text = newDir.absolutePath
    initTable(newDir)

    setTableFocusOn(focusedPath)

    updateStatusBar(newDir)

    eventBus.publish(CurrentDirChange(panelId, Option(prevDir), newDir))
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

      case KeyCode.S if !event.altDown && event.controlDown =>
        showFilterPopup()

      case _ =>
    }
  }

  private def handleAction(path: VPath): Unit = {
    if (path.isDirectory)
      changeDir(path.asInstanceOf[VDirectory])
    else {
      val fileType = fileTypeMgr.detectFileType(path)
      val maybeHandler = fileTypeMgr.fileTypeHandler(path)
      logger.debug(s"file action $path, fileType=$fileType, fileTypeActionHandler=$maybeHandler")
      for (handler <- maybeHandler) try {
        handler.handle(path)
      } catch {
        case e: IOException => logger.info(s"Error while opening file $path by default OS/Desktop environment application. Most probably there's no association defined for this file.", e)
      }

    }
  }

  private def changeDir(directory: VDirectory): Unit = {
    val prevDir = curDir
    curDir = directory
    val selection = directory match {
      case p: PathToParent => Some(prevDir)
      case _ => None
    }
    setCurrentDirectory(directory, selection)
  }

  private def showFilterPopup(): Unit = {
    val tf = new TextField() {
      prefWidth = dirTableView.width.value
    }
    val popup = new Popup() {
      content += tf
      autoHide = true
      // TODO: disable configuration changes while popup is opened? is it necessary?
      onHiding = e => filteredRows.predicate = createShowHiddenFilesPredicate()
    }
    var pending = false
    tf.handleEvent(KeyEvent.KeyPressed) { ke: KeyEvent =>
      try {
        val curFilterPredicate = filteredRows.predicate.value
        if (!pending) {
          pending = true
          ke.code match {
            case KeyCode.Escape =>
              popup.hide()
            case KeyCode.Up | KeyCode.Down | KeyCode.Enter =>
              setTableFocusOn(Some(dirTableView.selectionModel.value.getSelectedItem.path))
//              dirTableView.fireEvent(ke)
              popup.hide()
            case _ =>
              val filterNamePredicate: Predicate[_ >: FileRow] = r => r.path.name.toLowerCase.contains(tf.text.value.toLowerCase)
//              filteredRows.predicate = curFilterPredicate and filterNamePredicate
              // TODO: combine predicates with "and"
              filteredRows.predicate = row => curFilterPredicate.test(row) && filterNamePredicate.test(row)
          }
        }
      } finally {
        pending = false
      }
    }
    val bounds = dirTableView.localToScreen(dirTableView.boundsInLocal.value)
    popup.show(appController.mainStage, bounds.minX, bounds.maxY)
  }

  private def initTable(directory: VDirectory) {
    val (dirs0, files0) = directory.children.partition(p => p.isDirectory)

    val dirs =
      (if (directory.parent.isDefined) Seq(new PathToParent(directory)) else Seq()) ++
      dirs0.sortBy(d => d.name)
    val files = files0.sortBy(f => f.name)

    val fileRows = (dirs ++ files).view
      .map(f => new FileRow(f, resourceMgr))
      .toList

    tableRows.setAll(fileRows.asJava)
  }

  private def selectIndex(selIndex: Int): Unit = {
    val prevSelection = dirTableView.getSelectionModel.getSelectedIndex
    if (prevSelection != selIndex) {
      dirTableView.getSelectionModel.select(math.max(selIndex, 0))
      dirTableView.scrollTo(math.max(selIndex - NumPrevVisibleItems, 0))
    }
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