package org.mikesajak.commander

import java.io.IOException
import javafx.scene.control
import javafx.scene.control.SingleSelectionModel

import com.google.inject.name.Names
import com.google.inject.{AbstractModule, Guice}
import net.codingwell.scalaguice.ScalaModule
import org.mikesajak.commander.PanelId.{LeftPanel, RightPanel}

import scalafx.Includes._
import scalafx.application.Platform
import scalafx.scene.Node
import scalafx.scene.control.{Button, SplitPane, Tab, TabPane}
import scalafxml.core.macros.sfxml
import scalafxml.core.{FXMLLoader, NoDependencyResolver}
import scalafxml.guice.GuiceDependencyResolver

/**
 * Created by mike on 25.10.14.
 */
@sfxml
class MainPanelController(view: Button,
                           edit: Button,
                           copy: Button,
                           move: Button,
                           mkdir: Button,
                           delete: Button,
                           exit: Button,

                           dirsSplitPane: SplitPane,

                           leftTabPane: TabPane,
                           rightTabPane: TabPane,

                           private val buttonsCtrl: ButtonBarController,
                           private val panelsCtrl: PanelsController) {
  private val dirPanelLayout = "/layout/file-group-panel.fxml"

  buttonsCtrl.configureButtons(view, edit, copy, move, mkdir, delete, exit)

//  panelsCtrl.configurePanels(leftTabPane, rightTabPane)

  dirsSplitPane.items.clear()
  val leftPanel = createFilePanel(LeftPanel)
  val rightPanel = createFilePanel(RightPanel)
  dirsSplitPane.items += leftPanel
  dirsSplitPane.items += rightPanel

  private def createFilePanel(panelId: PanelId) = {
    implicit val injector = Guice.createInjector(new ApplicationContext,
                                                new PanelContext(DirPanelParams(panelId)))

    val resource = getClass.getResource(dirPanelLayout)
    if (resource == null)
      throw new IOException(s"Cannot load resource: $dirPanelLayout")

    val loader = new FXMLLoader(resource, new GuiceDependencyResolver())
    loader.load

    val root = loader.getRoot[javafx.scene.Parent]
    root
  }

}

class PanelContext(dirPanelParams: DirPanelParams) extends AbstractModule with ScalaModule {
  def configure() = {
    bind(classOf[DirPanelParams]).toInstance(dirPanelParams)
  }
}


//object MainPanelControllerImpl {
//  def configureButtonBar(viewButton: Button, editButton: Button, copyButton: Button, moveButton: Button,
//           mkdirButton: Button, deleteButton: Button, exitButton: Button) = {
//
//    // bind bottom toolbar buttons
//
//    // temporarily disable buttons with not implemented actions
//    viewButton.disable = true
//    editButton.disable = true
//    copyButton.disable = true
//    moveButton.disable = true
//    mkdirButton.disable = true
//    deleteButton.disable = true
//    exitButton.onAction = handle {
//      Platform.exit()
//    }
//  }
//
//  def configureTabPanels(leftTabPane: TabPane, rightTabPane: TabPane) = {
//
//    leftTabPane.tabs.clear()
//    rightTabPane.tabs.clear()
//
//    leftTabPane.selectionModel().selectedItemProperty.onChange {
//
//      val selection = leftTabPane.selectionModel.value
//      println("Left selection change: " + selection)
//    }
//
//    leftTabPane += createTab("Left example tab")
//    rightTabPane += createTab("Right example tab")
//
//    //  rightTabPane += createTab("Right example tab 1")
//    //  rightTabPane += createTab("Right example tab 2")
//
//    def createTab(title: String /* todo: other properties */) = {
//      val resource = getClass.getResource("/layout/file-tab-layout.fxml")
//      if (resource == null)
//        throw new IOException("Cannot load resource: file-tab-layout.fxml")
//
//      val loader = new FXMLLoader(resource, NoDependencyResolver)
//      loader.load
//
//      val root = loader.getRoot[javafx.scene.Parent]
//
//      leftTabPane.scaleShape = true
//
//      val tab = new Tab {
//        text = title
//        val pane = new Node(root){}
//        content = pane
//      }
//      tab
//    }
//  }
//}