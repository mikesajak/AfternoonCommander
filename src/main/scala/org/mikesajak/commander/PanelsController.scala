package org.mikesajak.commander

import java.io.IOException

import scalafx.scene.Node
import scalafx.scene.control.{Tab, TabPane}
import scalafxml.core.FXMLLoader
import scalafxml.guice.GuiceDependencyResolver

/**
  * Created by mike on 09.04.17.
  */
class PanelsController {
  private val dirTableLayout = "/layout/file-tab-layout.fxml"
  private val dirPanelLayout = "/layout/file-group-panel.fxml"
  private implicit val injector = ApplicationContext.globalInjector.createChildInjector()

  def configurePanels(leftTabPane: TabPane, rightTabPane: TabPane): Unit = {
    leftTabPane.tabs.clear()
    rightTabPane.tabs.clear()

    leftTabPane.selectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) => {
      println(s"Left selection change: ${newTab.getText}")
    })

    rightTabPane.selectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) => {
      val selection = leftTabPane.selectionModel.value
      println(s"Right selection change: ${newTab.getText}")
    })

    leftTabPane += createTab("Left example tab")
    rightTabPane += createTab("Right example tab")

    rightTabPane += createTab("Right example tab 1")
    rightTabPane += createTab("Right example tab 2")

    leftTabPane.scaleShape = true // ??? wtf is this ???

    createFilePanel()
  }

  private def createTab(title: String /* todo: other properties */) = {
    val resource = getClass.getResource(dirTableLayout)
    if (resource == null)
      throw new IOException(s"Cannot load resource: $dirTableLayout")

    val loader = new FXMLLoader(resource, new GuiceDependencyResolver())
    loader.load

    val root = loader.getRoot[javafx.scene.Parent]

    val tab = new Tab {
      text = title
      val pane = new Node(root){}
      content = pane
    }
    tab
  }

  private def createFilePanel() = {
    val resource = getClass.getResource(dirPanelLayout)
    if (resource == null)
      throw new IOException(s"Cannot load resource: $dirPanelLayout")

    val loader = new FXMLLoader(resource, new GuiceDependencyResolver())
    loader.load

    val root = loader.getRoot[javafx.scene.Parent]

    root
  }

}
