package org.mikesajak.commander.util

import java.io.BufferedOutputStream
import java.nio.file.{Files, Path, Paths, StandardOpenOption}

sealed abstract class SizeUnit(val multiplier: Long)
object SizeUnit {
  case object B extends SizeUnit(1)
  case object kB extends SizeUnit(1024)
  case object MB extends SizeUnit(kB.multiplier * 1024)
  case object GB extends SizeUnit(MB.multiplier * 1024)
  case object TB extends SizeUnit(GB.multiplier * 1024)


}

object BigFileCreator {

  val FileSizeMB = 100

  def main(args: Array[String]): Unit = {
    val path = Paths.get("./test/")
    val filePath = Files.createTempFile(path, "testFile", ".data")
    val numBytesToWrite = FileSizeMB * 1024 * 1024

    mkFile(filePath, numBytesToWrite)
  }

  def mkFile(filePath: Path, numBytesToWrite: Int) = {
    val stream = new BufferedOutputStream(Files.newOutputStream(filePath, StandardOpenOption.WRITE))

    println(s"Creating file with size ${UnitFormatter.byteUnit(numBytesToWrite)}...")

    val data = Iterator.iterate(0xff.toByte)(p => 0xff.toByte)
//    def data = Stream.tabulate(numBytesToWrite)(i => 0xff.toByte)
    val step = 1024 * 1024 * 10

    val progress = new Progress(numBytesToWrite)
    data.take(numBytesToWrite).sliding(step, step).foreach { frag =>
      val frag = data.take(step)
      stream.write(frag.toArray)
      progress.add(step)
    }
    stream.close()
  }

  class Progress(target: Int) {
    private var current = 0L
    private var last = -1L

    def add(value: Int) = {
      current += value
      val percent = (current * 100) / target
      if (last != percent) {
        println(s"$percent% (${UnitFormatter.formatUnit(current)} written)")
        last = percent
      }
    }
  }

}
