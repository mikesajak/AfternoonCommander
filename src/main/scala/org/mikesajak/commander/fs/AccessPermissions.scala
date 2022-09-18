package org.mikesajak.commander.fs

import enumeratum.{Enum, EnumEntry}
import org.mikesajak.commander.fs.Permission.{EXECUTE, READ_DATA, WRITE_DATA}

import java.nio.file.attribute.{AclEntryPermission, PosixFileAttributeView, PosixFileAttributes, PosixFilePermission}
import scala.collection.immutable
import scala.jdk.CollectionConverters.SetHasAsScala

sealed trait Permission extends EnumEntry

object Permission extends Enum[Permission] {
  val values: immutable.IndexedSeq[Permission] = findValues

  case object READ_DATA extends Permission
  case object WRITE_DATA extends Permission
  case object APPEND_DATA extends Permission
  case object EXECUTE extends Permission
  case object READ_NAMED_ATTRS extends Permission
  case object WRITE_NAMED_ATTRS extends Permission
  case object DELETE_CHILD extends Permission
  case object READ_ATTRIBUTES extends Permission
  case object WRITE_ATTRIBUTES extends Permission
  case object DELETE extends Permission
  case object READ_ACL extends Permission
  case object WRITE_ACL extends Permission
  case object WRITE_OWNER extends Permission
  case object SYNCHRONIZE extends Permission

  def apply(acl: AclEntryPermission): Permission = acl match {
    case AclEntryPermission.READ_DATA => READ_DATA
    case AclEntryPermission.WRITE_DATA => WRITE_DATA
    case AclEntryPermission.APPEND_DATA => APPEND_DATA
    case AclEntryPermission.EXECUTE => EXECUTE
    case AclEntryPermission.READ_NAMED_ATTRS => READ_NAMED_ATTRS
    case AclEntryPermission.WRITE_NAMED_ATTRS => WRITE_NAMED_ATTRS
    case AclEntryPermission.DELETE_CHILD => DELETE_CHILD
    case AclEntryPermission.READ_ATTRIBUTES => READ_ATTRIBUTES
    case AclEntryPermission.WRITE_ATTRIBUTES => WRITE_ATTRIBUTES
    case AclEntryPermission.DELETE => DELETE
    case AclEntryPermission.READ_ACL => READ_ACL
    case AclEntryPermission.WRITE_ACL => WRITE_ACL
    case AclEntryPermission.WRITE_OWNER => WRITE_OWNER
    case AclEntryPermission.SYNCHRONIZE => SYNCHRONIZE
  }
}

class AccessPermissions(val owner: String, val permissions: Map[String, Set[Permission]] = Map.empty)

class UnixAccessPermissions(override val owner: String, val group: String, override val permissions: Map[String, Set[Permission]])
  extends AccessPermissions(owner, permissions) {

  def ownerPermissions: Set[Permission] = permissions(s"owner: $owner")
  def groupPermissions: Set[Permission] = permissions(s"group: $group")
  def othersPermissions: Set[Permission] = permissions("others")
}

object AccessPermissions {
  def apply(posixFileAttributeView: PosixFileAttributeView): UnixAccessPermissions = {
    val attributes = posixFileAttributeView.readAttributes()
    new UnixAccessPermissions(attributes.owner().getName, attributes.group().getName, from(attributes))
  }

  private def from(posixAttribs: PosixFileAttributes): Map[String, Set[Permission]] = {
    posixAttribs.permissions().asScala.toSet
                .map(p => resolvePosixPermission(posixAttribs, p))
                .groupBy(p => p._1)
                .map(p => p._1 -> p._2.map(_._2))
  }

  private def resolvePosixPermission(posixAttribs: PosixFileAttributes, perm: PosixFilePermission) = perm match {
    case PosixFilePermission.OWNER_READ => "owner: " + posixAttribs.owner().getName -> READ_DATA
    case PosixFilePermission.OWNER_WRITE => "owner: " + posixAttribs.owner().getName -> WRITE_DATA
    case PosixFilePermission.OWNER_EXECUTE => "owner: " + posixAttribs.owner().getName -> EXECUTE

    case PosixFilePermission.GROUP_READ => "group: " + posixAttribs.group().getName -> READ_DATA
    case PosixFilePermission.GROUP_WRITE => "group: " + posixAttribs.group().getName -> WRITE_DATA
    case PosixFilePermission.GROUP_EXECUTE => "group: " + posixAttribs.group().getName -> EXECUTE

    case PosixFilePermission.OTHERS_READ => "others" -> READ_DATA
    case PosixFilePermission.OTHERS_WRITE => "others" -> WRITE_DATA
    case PosixFilePermission.OTHERS_EXECUTE => "others" -> EXECUTE
  }
}