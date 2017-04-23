package org.mikesajak.commander.ui.controller

import javax.swing.filechooser.FileSystemView

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import org.mikesajak.commander.config.Configuration
import org.mikesajak.commander.fs.local.LocalFS
import org.mikesajak.commander.fs.{FsMgr, VDirectory}
import org.mikesajak.commander.ui.UILoader

import scalafx.scene.Node
import scalafx.scene.control._
import scalafxml.core.macros.sfxml

sealed trait PanelId
object PanelId {
  case object LeftPanel extends PanelId
  case object RightPanel extends PanelId
}


trait DirPanelControllerInterface {
  def init(panelId: String)
}
/**
  * Created by mike on 14.04.17.
  */
@sfxml
class DirPanelController(tabPane: TabPane,
                         curDirField: TextField,
                         favDirsButton: Button,
                         backDirButton: Button,
                         topDirButton: Button,
                         drivesCombo: ComboBox[String],
                         freeSpaceLabel: Label,
                         showHiddenToggleButton: ToggleButton,
                         config: Configuration,
                         fsMgr: FsMgr)
    extends DirPanelControllerInterface {

  private val dirTableLayout = "/layout/file-tab-layout.fxml"

  println(s"DirPanelController - config=$config")

  def init(panelId: String) {
    tabPane.tabs.clear()

    tabPane.selectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) => {
      println(s"Left selection change: ${newTab.getText}")
    })

    val numTabs = config.intProperty(s"${panelId}.numTabs").getOrElse(0)

    val tabPathNames =
      if (numTabs != 0) {
        for (i <- 0 until numTabs) yield {
          val tabPath = config.stringProperty(s"panelId.tab[$i].path").get
          tabPath
        }
      } else {
        val fsv = FileSystemView.getFileSystemView
        val path = LocalFS.mkLocalPathName(fsv.getHomeDirectory.getAbsolutePath)
        List(path)
      }


    val tabPaths = tabPathNames
      .flatMap(path => fsMgr.resolvePath(path))
      // todo log problem with path
      .map(vpath => vpath.directory)

    println(s"tabPathNames=$tabPathNames, tabPaths=$tabPaths")


    tabPaths
      .map(createTab)
      .foreach(t => tabPane += t)

    val selectedPath = tabPaths.head
    // todo - selection
    curDirField.text = selectedPath.absolutePath
    tabPane.selectionModel.select(selectedPath.name)
    tabPane.getSelectionModel.selectFirst()
  }

  private def createTab(path: VDirectory) = {
    val root = UILoader.loadScene(dirTableLayout,
                                  new DirTableContext(DirTableParams(path)))

    val tab = new Tab {
      text = path.name
      val pane = new Node(root){}
      content = pane
    }
    tab
  }
}

case class DirPanelParams(panelId: PanelId)

class DirTableContext(dirTableParams: DirTableParams) extends AbstractModule with ScalaModule {
  def configure() = {
    bind(classOf[DirTableParams]).toInstance(dirTableParams)
  }
}
