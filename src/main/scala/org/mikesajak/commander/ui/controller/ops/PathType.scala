package org.mikesajak.commander.ui.controller.ops

sealed abstract class PathType(val name: String, val icon: String)
case object SingleFile extends PathType("file", "file-24.png")
case object SingleDir extends PathType("directory", "folder-24.png")
case object MultiPaths extends PathType("paths", "folder-multiple-24.png")
