package org.mikesajak.commander.ui

import java.io.IOException
import java.util.ResourceBundle

import com.google.inject.{Injector, Module}
import javafx.scene.Parent
import org.mikesajak.commander.ApplicationContext
import scalafxml.core.FXMLLoader
import scalafxml.guice.GuiceDependencyResolver

/**
  * Created by mike on 23.04.17.
  */
object UILoader {

  val defaultStyles = List("ui.css", "piechart.css")
  val defaultResource = "ui"

  def loadScene[CtrlType](layout: String, additionalContexts: Module*): (Parent, CtrlType) =
    loadScene[CtrlType](layout, defaultStyles, defaultResource, additionalContexts: _*)

  def loadScene[CtrlType](layout: String, styles: Seq[String], resourceBundle: String, additionalContexts: Module*): (Parent, CtrlType) = {
    val (root, loader)= loadSceneImpl(layout, styles, resourceBundle, additionalContexts: _*)
    val controller = loader.getController[CtrlType]()
    (root, controller)
  }

  def loadSceneNoCtrl[CtrlType](layout: String, additionalContexts: Module*): Parent = {
    val (root, _)= loadSceneImpl(layout, defaultStyles, defaultResource, additionalContexts: _*)
    root
  }

  private def loadSceneImpl(layout: String, styles: Seq[String], resourceBundle: String, additionalContexts: Module*) = {
    implicit val injector: Injector = ApplicationContext.globalInjector.createChildInjector(additionalContexts: _*)

    val resource = getClass.getResource(layout)
    if (resource == null)
      throw new IOException(s"Cannot load resource: $layout")

    val loader = new FXMLLoader(resource, new GuiceDependencyResolver())
    loader.setResources(ResourceBundle.getBundle(resourceBundle))
    loader.load()

    val root = loader.getRoot[Parent]()
    styles.foreach(css => root.getStylesheets.add(getClass.getResource(s"/$css").toExternalForm))

    (root,loader)
  }
}
