package org.mikesajak.commander

import java.io.IOException
import javax.swing.filechooser.FileSystemView

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import org.mikesajak.commander.config.Configuration
import org.mikesajak.commander.fs.local.LocalFS
import org.mikesajak.commander.fs.{FsMgr, VDirectory}

import scalafx.scene.Node
import scalafx.scene.control._
import scalafxml.core.FXMLLoader
import scalafxml.core.macros.sfxml
import scalafxml.guice.GuiceDependencyResolver

sealed trait PanelId
object PanelId {
  case object LeftPanel extends PanelId
  case object RightPanel extends PanelId
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
                         params: DirPanelParams,
                         config: Configuration) {

  private val dirTableLayout = "/layout/file-tab-layout.fxml"

  println(s"DirPanelController - params=$params")
  init()

  private def init() {
    tabPane.tabs.clear()

    tabPane.selectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) => {
      println(s"Left selection change: ${newTab.getText}")
    })

    //  tabPane += createTab("Left example tab")

    val numTabs = config.intProperty(s"${params.panelId}.numTabs").getOrElse(0)

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
      .flatMap(path => FsMgr.resolvePath(path))
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
    tabPane.getSelectionModel().selectFirst()
  }


  private def createTab(path: VDirectory) = {
    implicit val injector = ApplicationContext.globalInjector.createChildInjector(new DirTableContext(DirTableParams(path)))

    val resource = getClass.getResource(dirTableLayout)
    if (resource == null)
      throw new IOException(s"Cannot load resource: $dirTableLayout")

    val loader = new FXMLLoader(resource, new GuiceDependencyResolver())
    loader.load

    val root = loader.getRoot[javafx.scene.Parent]

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
