package org.mikesajak.commander

import java.io.IOException

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import org.mikesajak.commander.PanelId.{LeftPanel, RightPanel}

import scalafx.Includes._
import scalafx.scene.control.{Button, SplitPane, TabPane}
import scalafxml.core.FXMLLoader
import scalafxml.core.macros.sfxml
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
    implicit val injector = ApplicationContext.globalInjector.createChildInjector(new PanelContext(DirPanelParams(panelId)))

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
