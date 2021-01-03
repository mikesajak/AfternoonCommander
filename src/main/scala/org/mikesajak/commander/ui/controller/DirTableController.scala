package org.mikesajak.commander.ui.controller

import java.util.function.Predicate

import com.google.common.eventbus.Subscribe
import com.typesafe.scalalogging.Logger
import org.mikesajak.commander.config.{ConfigKeys, ConfigObserver, Configuration}
import org.mikesajak.commander.fs._
import org.mikesajak.commander.handler.{ActionFileHandler, ContainerFileHandler, FileHandlerFactory}
import org.mikesajak.commander.ui.controller.DirViewEvents.CurrentDirChangeNotification
import org.mikesajak.commander.ui.{IconResolver, PropertiesCtrl, ResourceManager}
import org.mikesajak.commander.units.DataUnit
import org.mikesajak.commander.util.PathUtils
import org.mikesajak.commander.{ApplicationController, EventBus, FileTypeManager}
import scalafx.Includes._
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.collections.transformation.{FilteredBuffer, SortedBuffer}
import scalafx.geometry.Side
import scalafx.scene.control._
import scalafx.scene.input.{KeyCode, KeyEvent, MouseButton, MouseEvent}
import scalafx.stage.Popup
import scalafxml.core.macros.{nested, sfxml}

import scala.jdk.CollectionConverters._

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

  def formatSize(pathToFormat: VPath): String =
    pathToFormat match {
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

trait DirTableController {
  def init(path: VDirectory)
  def dispose()
  def currentDirectory: VDirectory
  def setCurrentDirectory(path: VDirectory, focusedPath: Option[VPath] = None)
  def focusedPath: VPath
  def selectedPaths: Seq[VPath]
  def reload(): Unit
  def setTableFocusOn(pathOption: Option[VPath])
  def setTableFocusOn(pathName: String) // TODO: change to some matcher, or first occurrence, or startsWith etc.

  def requestFocus()
}

@sfxml
class DirTableControllerImpl(dirTableView: TableView[FileRow],
                             idColumn: TableColumn[FileRow, VPath],
                             nameColumn: TableColumn[FileRow, String],
                             extColumn: TableColumn[FileRow, String],
                             sizeColumn: TableColumn[FileRow, String],
                             dateColumn: TableColumn[FileRow, String],
                             attribsColumn: TableColumn[FileRow, String],

                             @nested[PanelActionsBarControllerImpl] panelActionsBarController: PanelActionsBarController,
                             @nested[PathBarControllerImpl] curPathBarController: PathBarController,
                             @nested[PanelStatusBarControllerImpl] panelStatusBarController: PanelStatusBarController,

                             panelId: PanelId,
                             fileTypeMgr: FileTypeManager,
                             fileHandlerFactory: FileHandlerFactory,
                             resourceMgr: ResourceManager,
                             iconResolver: IconResolver,
                             config: Configuration,
                             eventBus: EventBus,
                             appController: ApplicationController,
                             propertiesCtrl: PropertiesCtrl)
    extends DirTableController {

  import org.mikesajak.commander.ui.UIParams._

  private val logger = Logger[DirTableController]

  private var curDir: VDirectory = _

  private val tableRows = ObservableBuffer[FileRow]()
  private val filteredRows = new FilteredBuffer(tableRows)
  private val sortedRows = new SortedBuffer(filteredRows)

  private val configObserver: ConfigObserver = new ConfigObserver {
    //noinspection ScalaUnusedSymbol
    val observedKeyPrefix: String = ConfigKeys.filePanelCategory

    //noinspection UnstableApiUsage
    @Subscribe
    override def configChanged(key: String): Unit =
      key match {
        case ConfigKeys.ShowHiddenFiles =>
          filteredRows.predicate = createShowHiddenFilesPredicate()

        case key if key startsWith ConfigKeys.filePanelColumnCategory =>
          handleTableColumnWidthChange(key)

        case _ => // ignore all other changes, config keys that this panel is not interested
      }

    def handleTableColumnWidthChange(configKey: String): Unit = {
      val column = configKey match {
        case ConfigKeys.NameColumnWidth => Some(nameColumn)
        case ConfigKeys.ExtColumnWidth => Some(extColumn)
        case ConfigKeys.SizeColumnWidth => Some(sizeColumn)
        case ConfigKeys.ModifiedColumnWidth => Some(dateColumn)
        case ConfigKeys.AttribsColumnWidth => Some(attribsColumn)
        case _ =>
          logger.debug(s"Width change for unknown column. ConfigKey: $configKey")
          None
      }

      column.foreach { c => config.intProperty(configKey).value.foreach { width => c.prefWidth = width } }
    }
  }

  override def focusedPath: VPath = dirTableView.selectionModel.value.getSelectedItem.path

  override def selectedPaths: Seq[VPath] = dirTableView.selectionModel.value.getSelectedItems.map(_.path).toSeq

  override def init(path: VDirectory) {
    dirTableView.selectionModel.value.selectionMode = SelectionMode.Multiple

    dirTableView.selectionModel.value.selectedItems.onChange {
      val selectedPaths = dirTableView.selectionModel.value.selectedItems
                                      .map(_.path)
                                      .toSeq
      panelStatusBarController.setSelectedPaths(selectedPaths)
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

    initColumnWidthChangeHandlers()

    dirTableView.handleEvent(KeyEvent.KeyPressed) { event: KeyEvent => handleKeyEvent(event) }

    eventBus.register(configObserver)
    filteredRows.predicate = createShowHiddenFilesPredicate()

    dirTableView.items = sortedRows

    panelStatusBarController.init()
    curPathBarController.init(directory => setCurrentDirectory(directory))
    panelActionsBarController.init(directory => setCurrentDirectory(directory))

    setCurrentDirectory(path)
  }

  private def initColumnWidthChangeHandlers(): Unit = {
    bindColumnWidthToConfigChanges(nameColumn, ConfigKeys.NameColumnWidth)
    bindColumnWidthToConfigChanges(extColumn, ConfigKeys.ExtColumnWidth)
    bindColumnWidthToConfigChanges(sizeColumn, ConfigKeys.SizeColumnWidth)
    bindColumnWidthToConfigChanges(dateColumn, ConfigKeys.ModifiedColumnWidth)
    bindColumnWidthToConfigChanges(attribsColumn, ConfigKeys.AttribsColumnWidth)
  }

  private def bindColumnWidthToConfigChanges(column: TableColumn[_,_], configKey: String): Unit = {
    config.intProperty(configKey).value.foreach(width => column.prefWidth = width)

    column.width.onChange { (_, _, newSize) => config.intProperty(configKey) := newSize.intValue }
  }

  override def dispose(): Unit = {
    eventBus.unregister(configObserver)
  }

  private def createShowHiddenFilesPredicate() = {
    row: FileRow =>
      val showHidden = config.boolProperty(ConfigKeys.ShowHiddenFiles).getOrElse(false)
      row.path.isInstanceOf[PathToParent] ||
        !row.path.attributes.contains(Attrib.Hidden) || showHidden
  }

  override def currentDirectory: VDirectory = curDir

  override def setCurrentDirectory(directory: VDirectory, focusedPath: Option[VPath] = None): Unit = {
    val prevDir = curDir
    val newDir = resolveTargetDir(directory)
    curDir = newDir

    curPathBarController.setDirectory(newDir)

    initTable(newDir)

    setTableFocusOn(focusedPath)

    panelStatusBarController.setDirectory(newDir)

    eventBus.publish(CurrentDirChangeNotification(panelId, Option(prevDir), newDir))
  }

  private def resolveTargetDir(directory: VDirectory): VDirectory = {
    directory match {
      case p: PathToParent => p.targetDir
      case _ => directory
    }
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

  override def requestFocus(): Unit = {
    dirTableView.requestFocus()
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
      filteredRows.predicate = (row: FileRow) => curFilterPredicate.test(row) && filterNamePredicate.test(row)
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
      dirTableView.getSelectionModel.select(math.max(index, 0), nameColumn) // column is needed in selection to prevent bug in javafx implementation (NPE for null column)
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
}