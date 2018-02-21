package org.mikesajak.commander.ui

import java.io.IOException
import java.util.ResourceBundle
import javafx.scene.Parent

import com.google.inject.{Injector, Module}
import org.mikesajak.commander.ApplicationContext

import scalafxml.core.FXMLLoader
import scalafxml.guice.GuiceDependencyResolver

/**
  * Created by mike on 23.04.17.
  */
object UILoader {

  def loadScene[CtrlType](layout: String, additionalContexts: Module*): (Parent, CtrlType) =
    loadScene[CtrlType](layout, "ui.css", "ui", additionalContexts: _*)

  def loadScene[CtrlType](layout: String, css: String, resourceBundle: String, additionalContexts: Module*): (Parent, CtrlType) = {
    implicit val injector: Injector = ApplicationContext.globalInjector.createChildInjector(additionalContexts: _*)

    val resource = getClass.getResource(layout)
    if (resource == null)
      throw new IOException(s"Cannot load resource: $layout")

    val loader = new FXMLLoader(resource, new GuiceDependencyResolver())
    loader.setResources(ResourceBundle.getBundle(resourceBundle))
    loader.load()

    val root = loader.getRoot[Parent]()
    root.getStylesheets.add(getClass.getResource(s"/$css").toExternalForm)

    val controller = loader.getController[CtrlType]()
    (root, controller)
  }
}
