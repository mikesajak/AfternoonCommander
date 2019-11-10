package org.mikesajak.commander.task

import enumeratum.{Enum, EnumEntry}
import org.mikesajak.commander.fs.VPath

import scala.collection.immutable

case class TransferJob(pathsToCopy: Seq[TransferDef], operationType: OperationType, preserveModificationDate: Boolean)

object TransferJob {
  def apply(source: VPath, target: VPath, opType: OperationType, preserveModificationDate: Boolean) =
    new TransferJob(Seq(TransferDef(source, target)), opType, preserveModificationDate)
}

sealed abstract class OperationType extends EnumEntry
object OperationType extends Enum[OperationType] {
  override val values: immutable.IndexedSeq[OperationType] = findValues

  case object Copy extends OperationType
  case object Move extends OperationType
}

case class TransferDef(source: VPath, target: VPath)
