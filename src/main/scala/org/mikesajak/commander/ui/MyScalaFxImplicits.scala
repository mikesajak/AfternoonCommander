package org.mikesajak.commander.ui

import javafx.stage
import scalafx.scene.control.{Control, Dialog, Labeled}

import scala.language.implicitConversions

object MyScalaFxImplicits {
//  implicit def jfxButton2SfxButton(button: jfxctrl.Button): Button = new Button(button)

  implicit class RichDialog[A](val self: Dialog[A]) {
    def setWindowSize(width: Int, height: Int): Unit = {
      val window = self.getDialogPane.getScene.getWindow.asInstanceOf[stage.Stage]
      window.setMinWidth(width)
      window.setWidth(width)
      window.setMinHeight(height)
      window.setHeight(height)
    }
  }

  implicit class ControlCollection[+A <: Control](val nodes: List[A]) {
    def visible: Boolean = nodes.forall(_.visible.value)
    def visible_=(vis: Boolean): Unit = nodes.foreach(_.visible = vis)
  }

  implicit class LabeledCollection[+A <: Labeled](override val nodes: List[A]) extends ControlCollection[A](nodes) {
    def text: String = nodes.foldLeft("")((acc, node) => s"$acc, ${node.text}")
    def text_=(str: String): Unit = nodes.foreach(_.text = str)
  }

  implicit def controlToCol[A <: Control, B <: Control](nodes: (A, B)): ControlCollection[Control] =
    new ControlCollection(List(nodes._1, nodes._2))

  implicit def controlToCol[A <: Control, B <: Control, C <: Control](nodes: (A, B, C)): ControlCollection[Control] =
    new ControlCollection(List(nodes._1, nodes._2, nodes._3))

  implicit def controlToCol[A <: Control, B <: Control, C <: Control, D <: Control](nodes: (A, B, C, D)): ControlCollection[Control] =
    new ControlCollection(List(nodes._1, nodes._2, nodes._3, nodes._4))

  implicit def controlToCol[A <: Control, B <: Control, C <: Control, D <: Control, E <: Control](nodes: (A, B, C, D, E)): ControlCollection[Control] =
    new ControlCollection(List(nodes._1, nodes._2, nodes._3, nodes._4, nodes._5))


  implicit def labeledToCol[A <: Labeled, B <: Labeled](nodes: (A, B)): LabeledCollection[Labeled] =
    new LabeledCollection(List(nodes._1, nodes._2))

  implicit def labeledToCol[A <: Labeled, B <: Labeled, C <: Labeled](nodes: (A, B, C)): LabeledCollection[Labeled] =
    new LabeledCollection(List(nodes._1, nodes._2, nodes._3))

  implicit def labeledToCol[A <: Labeled, B <: Labeled, C <: Labeled, D <: Labeled](nodes: (A, B, C, D)): LabeledCollection[Labeled] =
    new LabeledCollection(List(nodes._1, nodes._2, nodes._3, nodes._4))

  implicit def labeledToCol[A <: Labeled, B <: Labeled, C <: Labeled, D <: Labeled, E <: Labeled](nodes: (A, B, C, D, E)): LabeledCollection[Labeled] =
    new LabeledCollection(List(nodes._1, nodes._2, nodes._3, nodes._4, nodes._5))

}
