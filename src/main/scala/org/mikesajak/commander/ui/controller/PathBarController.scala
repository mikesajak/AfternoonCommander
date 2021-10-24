package org.mikesajak.commander.ui.controller

import javafx.scene.{control => jfxctrl}
import org.controlsfx.control.BreadCrumbBar
import org.mikesajak.commander.fs.VDirectory
import org.mikesajak.commander.util.PathUtils
import scalafx.Includes._
import scalafx.scene.control.Button
import scalafx.scene.layout.{HBox, VBox}
import scalafxml.core.macros.sfxml

import scala.annotation.tailrec

trait PathBarController {
  def init(listener: CurrentDirAware)
  def setDirectory(directory: VDirectory)
}

trait CurrentDirAware {
  def setDirectory(directory: VDirectory)
}

@sfxml
class PathBarControllerImpl(curPathPanel: HBox,
                            addNewTabButton: Button,

                            pathBreadCrumbWrapper: VBox,
                            pathBreadCrumbBar: BreadCrumbBar[BreadCrumbItem])
    extends PathBarController with CurrentDirAware {

  private var curDirListeners = List[CurrentDirAware]()

  def init(listener: CurrentDirAware): Unit = {
    curDirListeners ::= listener
    setupPathBreadCrumbBar()
  }

  private def setupPathBreadCrumbBar(): Unit = {
    pathBreadCrumbBar.setAutoNavigationEnabled(false)
    pathBreadCrumbWrapper.children.setAll(pathBreadCrumbBar)
    pathBreadCrumbBar.setCrumbFactory { item =>
      item.value.value match {
        case null => new BreadCrumbBar.BreadCrumbButton(null)
        case PathCrumbItem(p) => new BreadCrumbBar.BreadCrumbButton(p.name) {
          setTooltip(new jfxctrl.Tooltip(p.absolutePath))
        }
        case PrevCrumbItems(prevPaths) => new BreadCrumbBar.BreadCrumbButton("...") {
          setTooltip(new jfxctrl.Tooltip(s"${prevPaths.last.absolutePath}"))
        }
      }
      // TODO: mark filesystem directory with icon
    }

    pathBreadCrumbBar.setOnCrumbAction { event =>
      event.getSelectedCrumb.getValue match {
        case null =>
        case PathCrumbItem(path) => notifyDirectoryChange(path.directory)
        case x@PrevCrumbItems(paths) =>
          println(s"Hit prev .... $x")
          notifyDirectoryChange(paths.last.directory)
      }
    }
  }

  private def notifyDirectoryChange(newDir: VDirectory): Unit = {
    curDirListeners.foreach(_.setDirectory(newDir))
  }

  override def setDirectory(directory: VDirectory): Unit = {
    updatePathBreadCrumbBar(directory)
  }

  private def updatePathBreadCrumbBar(dir: VDirectory): Unit = {
    val pathToRoot = PathUtils.pathToRoot(dir)

    val items = pathToRoot.map(p => PathCrumbItem(p))
    pathBreadCrumbBar.setSelectedCrumb(BreadCrumbBar.buildTreeModel(items: _*))

    resizeCrumbBar()
  }

  private def resizeCrumbBar(): Unit = {
    recomputeSize(pathBreadCrumbBar)
    while (pathBreadCrumbBar.getWidth > curPathPanel.width.value) {
      val lastItem = pathBreadCrumbBar.getSelectedCrumb
      val (invisibleSegments, visibleSegments) = treePathToRoot(lastItem).map(_.getValue).reverse match {
        case PrevCrumbItems(prevItems) :: PathCrumbItem(headPath) :: tail => (headPath +: prevItems, tail)
        case PathCrumbItem(headPath) :: tail => (List(headPath), tail)
        case items => (List(), items)
      }

      val segments = PrevCrumbItems(invisibleSegments) :: visibleSegments
      pathBreadCrumbBar.setSelectedCrumb(BreadCrumbBar.buildTreeModel(segments: _*))

      recomputeSize(pathBreadCrumbBar)
    }
  }

  private def recomputeSize(control: jfxctrl.Control): Unit = {
    Option(control.getScene)
        .flatMap(scene => Option(scene.getRoot))
        .foreach { sceneRoot =>
          sceneRoot.applyCss()
          sceneRoot.layout()
          control.autosize()
        }
  }

  @tailrec
  private def treePathToRoot(item: jfxctrl.TreeItem[BreadCrumbItem],
                             curPath: List[jfxctrl.TreeItem[BreadCrumbItem]] = List())
      : List[jfxctrl.TreeItem[BreadCrumbItem]] =
    if (item.getParent != null) treePathToRoot(item.getParent, curPath :+ item)
    else curPath

}
