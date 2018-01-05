package org.mikesajak.commander.util

import java.io.BufferedOutputStream
import java.nio.file._
import java.util.concurrent.{ForkJoinPool, RecursiveAction}

import com.google.common.base.Stopwatch

object TestDataCreator {

  val NumTestDirs = 3
  val NumTestFiles = 5
  val TestFileSize = 1024 * 100

  val Depth = 5

  def main(args: Array[String]): Unit = {
    val rootPath = Paths.get("./test/test")

    if (!Files.exists(rootPath)) {
      Files.createDirectory(rootPath)
    }

    val randomNumber = false
    val randomFileSize = false
    println(s"Generating test $NumTestDirs(random=$randomNumber) dirs(, $NumTestFiles(random=$randomNumber) files with $TestFileSize(random=$randomFileSize) bytes each. Depth=$Depth")

    val stopwatch = Stopwatch.createStarted()

//    createDirTree(rootPath, NumTestDirs, NumTestFiles, false, TestFileSize, false, Depth, "")
    ForkJoinPool.commonPool().invoke(new CreateDirTreeRTask(rootPath, false, NumTestDirs, NumTestFiles, false, TestFileSize, false, Depth, ""))

    println(s"Done in $stopwatch")
  }

  def createDirTree(path: Path, numDirs: Int, numFiles: Int, random: Boolean, fileSize: Int, randomSize: Boolean, numLevels: Int, indent: String): Unit = {
    println(s"${indent}Processing $path")
    val numFilesToCreate = if (random) (math.random() * numFiles).toInt else numFiles

    println(s"${indent} Creating $numFilesToCreate files with ${if (randomSize) "up to " else ""} bytes each.")

    for (i <- 0 to numFilesToCreate) {
      val filePath = Files.createTempFile(path, "file", "")
      val numBytesToWrite = if (randomSize) (math.random() * fileSize).toInt + 1 else fileSize
      mkFile(filePath, numBytesToWrite)
    }

    if (numLevels > 0) {
      val numDirsToCreate = if (random) (math.random() * numDirs).toInt else numDirs

      println(s"${indent} Creating $numDirsToCreate dirs")
      for (i <- 0 to numDirsToCreate) {
        val dirPath = Files.createTempDirectory(path, "dir")
        createDirTree(dirPath, numDirs, numFiles, random, fileSize, randomSize, numLevels - 1, indent + "   ")
      }
    }

  }

  def mkFile(filePath: Path, numBytesToWrite: Int) = {
    val stream = new BufferedOutputStream(Files.newOutputStream(filePath, StandardOpenOption.WRITE))
    //      println(s"${indent}  Creating file with size ${numBytesToWrite}B")

    val data = Stream.tabulate(numBytesToWrite)(i => 0xff.toByte)
    data.sliding(1024, 1024).foreach(frag => stream.write(frag.toArray))
    stream.close()
  }

  class CreateDirTreeRTask(path: Path, createDir: Boolean, numDirs: Int, numFiles: Int, random: Boolean,
                           fileSize: Int, randomSize: Boolean, numLevels: Int, indent: String) extends RecursiveAction {
    override def compute(): Unit = {
      println(s"${indent}Processing $path")

      val dirPath = if (createDir) Files.createTempDirectory(path, "dir") else path

      if (numLevels > 0) {
        val numDirsToCreate = if (random) (math.random() * numDirs).toInt else numDirs

        println(s"${indent} Creating $numDirsToCreate dirs")
        val childTasks =
          for (i <- 0 to numDirs) yield
            new CreateDirTreeRTask(dirPath, true, numDirs, numFiles, random,
              fileSize, randomSize, numLevels-1, indent+"    ").fork()

        childTasks.foreach(_.join())
      }

      val numFilesToCreate = if (random) (math.random() * numFiles).toInt else numFiles

      println(s"${indent} Creating $numFilesToCreate files with ${if (randomSize) "up to " else ""} bytes each.")

      for (i <- 0 to numFilesToCreate) {
        val filePath = Files.createTempFile(dirPath, "file", "")
        val numBytesToWrite = if (randomSize) (math.random() * fileSize).toInt + 1 else fileSize
        mkFile(filePath, numBytesToWrite)
      }
    }
  }

}
