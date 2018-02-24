package org.mikesajak.commander.ui

import org.mikesajak.commander.fs.FS

object FSUIHelper {
  def findIconFor(fs: FS, size: Int): String = {
    val basename = fs.attributes.get("usb").map(_ => "usb")
      .orElse(fs.attributes.get("removable").map(_ => "usb"))
      .getOrElse {
        fs.attributes("type") match {
          case "vfat" | "fat" | "fat16" | "fat32" | "fat64" | "exfat" => "usb"
          case "iso9660" => "disk"
          case "ext2" | "ext3" | "ext4" | "btrfs" | "reiserfs" | "zfs" | "xfs" | "jfs"
               | "ntfs" => "harddisk"
          case _ => "harddisk"
        }
      }
    s"$basename-$size.png"
  }
}
