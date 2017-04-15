package org.mikesajak.commander

import java.io.IOException
import javafx.{scene => jfxs}

import com.google.inject.Guice

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.{Node, Scene}
import scalafxml.core.{FXMLLoader, FXMLView}
import scalafxml.guice.GuiceDependencyResolver


/**
 * Created by mike on 25.10.14.
 */
object Main extends JFXApp {
  private val mainPanelDef: String = "/layout/main-panel-layout.fxml"
  val resource = getClass.getResource(mainPanelDef)
  if (resource == null)
    throw new IOException(s"Cannot load resource: $mainPanelDef")

//  val root: Parent = FXMLView(resource, NoDependencyResolver)
  implicit val injector = Guice.createInjector(new ApplicationContext)
  val loader = new FXMLLoader(resource, new GuiceDependencyResolver())

  loader.load()
  val root = loader.getRoot[jfxs.Parent]

  stage = new PrimaryStage() {
    title = "ScalaCommander"
//    scene = new Scene(new jfxs.Scene(root))
    scene = new Scene(
//        FXMLView(
//          getClass.getResource(mainPanelDef),
////          new Node(root),
//          new GuiceDependencyResolver()
////          new NoDependencyResolver()
////          new DependenciesByType(Map())
//        )
      root
    )
  }

  override def main(args: Array[String]): Unit = {
    super.main(args)
  }
}

