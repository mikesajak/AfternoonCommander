package org.mikesajak.commander.task

case class CancelledException[A](value: A) extends Exception {
  def this() = this(null.asInstanceOf[A])
}
