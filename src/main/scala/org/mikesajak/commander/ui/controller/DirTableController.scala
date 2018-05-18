package org.mikesajak.commander.ui.controller

import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.config.{ConfigKey, ConfigObserver, Configuration}
import org.mikesajak.commander.fs.{PathToParent, VDirectory, VFile, VPath}
import org.mikesajak.commander.ui.{ResourceManager, UIUtils}
import org.mikesajak.commander.util.TextUtil._
import org.mikesajak.commander.util.{PathUtils, UnitFormatter}
import org.mikesajak.commander.{ApplicationController, FileTypeManager}
import scalafx.Includes._
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.collections.transformation.{FilteredBuffer, SortedBuffer}
import scalafx.geometry.Insets
import scalafx.scene.control._
import scalafx.scene.effect.BlendMode
import scalafx.scene.image.ImageView
import scalafx.scene.input.{KeyCode, KeyEvent, MouseButton, MouseEvent}
import scalafx.scene.{CacheHint, Group, Node}
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
      case p: PathToParent => resourceMgr.getMessage("file_row.parent")
      case p: VDirectory => resourceMgr.getMessageWithArgs("file_row.num_elements",
                                                           Array(path.directory.children.size))
      case p: VFile => UnitFormatter.formatDataSize(path.size)
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
  def init(panelController: DirPanelControllerIntf, path: VDirectory)
  def dispose()
  def setCurrentDirectory(path: VDirectory, focusedPath: Option[VPath] = None)
  def focusedPath: VPath
  def selectedPaths: Seq[VPath]
  def reload(): Unit
  def select(fileName: String): Unit
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

                         fileTypeMgr: FileTypeManager,
                         resourceMgr: ResourceManager,
                         config: Configuration,
                         appController: ApplicationController)
    extends DirTableControllerIntf {

  import org.mikesajak.commander.ui.UIParams._

  private val logger = Logger[DirTableController]

  private var curDir: VDirectory = _
  private var panelController: DirPanelControllerIntf = _

  private val tableRows = ObservableBuffer[FileRow]()
  private val filteredRows = new FilteredBuffer(tableRows)
  private val sortedRows = new SortedBuffer(filteredRows)

  addTabButton.padding = Insets.Empty
  curDirField.prefHeight <== addTabButton.height

  private val showHiddenFilesPreditate = { row: FileRow =>
    row.path.isInstanceOf[PathToParent] ||
      !row.path.attributes.contains('h') ||
      config.boolProperty("file_panel", "show_hidden").getOrElse(false)
  }

  private val showHiddenConfigObserver: ConfigObserver = new ConfigObserver {
    override val observedKey = ConfigKey("file_panel", "show_hidden")
    override def configChanged(key: ConfigKey): Unit = key match {
      case ConfigKey("file_panel", "show_hidden") =>
        filteredRows.predicate = showHiddenFilesPreditate
      case _ => logger.info(s"Unexpected config change delivered to this observer: $key")
    }
  }

  override def focusedPath: VPath = dirTableView.selectionModel.value.getSelectedItem.path

  override def selectedPaths: ObservableBuffer[VPath] = dirTableView.selectionModel.value.getSelectedItems.map(_.path)

  override def init(dirPanelController: DirPanelControllerIntf, path: VDirectory) {
    this.panelController = dirPanelController

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
          graphic = Option(newValue).flatMap(findIconFor).orNull
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

    filteredRows.predicate = showHiddenFilesPreditate
    dirTableView.items = sortedRows

    config.registerObserver(showHiddenConfigObserver)

    addTabButton.onAction = e => dirPanelController.addNewTab()

    setCurrentDirectory(path)
  }

  override def dispose(): Unit = {
    config.unregisterObserver(showHiddenConfigObserver)
  }

  override def setCurrentDirectory(dir: VDirectory, focusedPath: Option[VPath] = None): Unit = {
    curDir = dir
    curDirField.text = curDir.absolutePath
    initTable(dir, focusedPath)
    updateStatusBar(dir)
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
    val sizeUnit = UnitFormatter.findDataSizeUnit(totalSize)
    resourceMgr.getMessageWithArgs("file_table_panel.status.message",
                                                       Array(numDirs, files.size,
                                                             sizeUnit.convert(totalSize),
                                                             sizeUnit.symbol))
  }

  override def reload(): Unit = {
    setCurrentDirectory(curDir, Some(focusedPath))
  }

  override def select(target: String): Unit = {
    val selIndex = {
      val idx = dirTableView.items.value.map(fileRow => fileRow.path.name).indexOf(target)
      if (idx > 0) idx else 0
    }

    selectIndex(selIndex)
  }

  private def findIconFor(path: VPath): Option[Node] = {
    val fileType = fileTypeMgr.detectFileType(path)
    fileType.icon.map { iconFile =>
      val imageView = new ImageView(resourceMgr.getIcon(iconFile, 18, 18))
      imageView.preserveRatio = true
      imageView.cache = true
      imageView.cacheHint = CacheHint.Speed

      if (path.attributes.contains('x') && !path.attributes.contains('d')) new Group(imageView, execOverlayIcon)
      else imageView
    }
  }

  private def execOverlayIcon: ImageView = {
    val asterisk = new ImageView(resourceMgr.getIcon("asterisk-green.png", 14, 14))
    asterisk.x = 6
    asterisk.y = 6
    asterisk.blendMode = BlendMode.SrcAtop
    asterisk.cache = true
    asterisk.cacheHint = CacheHint.Speed
    asterisk
  }

  private def handleKeyEvent(event: KeyEvent) {
    println(s"handleKeyEvent: $event")
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
      val fileTypeActionHandler = fileTypeMgr.fileTypeHandler(path)
      println(s"TODO: file action $path, fileType=$fileType, fileTypeActionHandler=$fileTypeActionHandler")
    }
  }

  private def changeDir(directory: VDirectory): Unit = {
    var prevDir = curDir
    curDir = directory
    updateParentTab(directory)
    val selection = directory match {
      case p: PathToParent => Some(prevDir)
      case _ => None
    }
    setCurrentDirectory(directory, selection)
  }

  private def updateParentTab(directory: VDirectory): Unit = {
    val targetDir = directory match {
      case p: PathToParent => p.targetDir
      case _ => directory
    }
    panelController.updateCurTab(targetDir)
  }

  private def showFilterPopup(): Unit = {
    val tf = new TextField() {
      prefWidth = dirTableView.width.value
    }
    val popup = new Popup() {
      content += tf
      autoHide = true
      onHiding = e => filteredRows.predicate = showHiddenFilesPreditate
    }
    var pending = false
    tf.handleEvent(KeyEvent.KeyPressed) { ke: KeyEvent =>
      try {
        if (!pending) {
          pending = true
          ke.code match {
            case KeyCode.Escape =>
              popup.hide()
            case KeyCode.Up | KeyCode.Down | KeyCode.Enter =>
              select(dirTableView.selectionModel.value.getSelectedItem.path.name)
//              dirTableView.fireEvent(ke)
              popup.hide()
            case _ =>
              filteredRows.predicate = row => {
                showHiddenFilesPreditate(row) && row.path.name.toLowerCase.contains(tf.text.value.toLowerCase)
              }
          }
        }
      } finally {
        pending = false
      }
    }
    val bounds = dirTableView.localToScreen(dirTableView.boundsInLocal.value)
    popup.show(appController.mainStage, bounds.minX, bounds.maxY)
  }

  private def initTable(directory: VDirectory, selection: Option[VPath]) {
    val (dirs0, files0) = directory.children.partition(p => p.isDirectory)

    val dirs =
      (if (directory.parent.isDefined) Seq(new PathToParent(directory)) else Seq()) ++
      dirs0.sortBy(d => d.name)
    val files = files0.sortBy(f => f.name)

    val fileRows = (dirs ++ files).view
      .map(f => new FileRow(f, resourceMgr))
      .toList

    tableRows.setAll(fileRows.asJava)

    val fromDir = selection

    val selIndex = fromDir.map { prevDir =>
      val idx = sortedRows.indexWhere(row => row.path.name == prevDir.name)
      if (idx > 0) idx else 0
    }.getOrElse(0)

    selectIndex(selIndex)
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
    curDirField.text.onChange { (observable, oldVal, newVal) =>
      try {
        if (!pending) {
          pending = true
          var textWidth = UIUtils.calcTextBounds(curDirField).getWidth
          val componentWidth = curDirField.getBoundsInLocal.getWidth
          while (textWidth > componentWidth) {
            curDirField.text = shortenDirText(curDirField.text.value)
            textWidth = UIUtils.calcTextBounds(curDirField).getWidth
            curDirField.insets
          }
        }
      } finally {
        pending = false
      }
    }

    curDirField.width.onChange { (_, oldVal, newVal) =>
      curDirField.text = curDir.absolutePath
    }
  }

  private def shortenDirText(text: String): String = {
    // TODO: smarter shortening - e.g. cut the middle of the path, leave the beginning and ending etc. /root/directory/.../lastDir
    text.substring(1)
  }
}
