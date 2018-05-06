package org.mikesajak.commander.util

import java.nio.ByteBuffer
import java.nio.channels.{ReadableByteChannel, WritableByteChannel}

import org.mikesajak.commander.task.AbortOperationException

object IO {
  trait CopyListener {
    def bytesWritten(size: Int): Boolean
  }

  def channelCopy(source: ReadableByteChannel, target: WritableByteChannel, bufferSize: Int, copyListener: CopyListener): Unit =
    channelCopy(source, target, bufferSize, Some(copyListener))

  def channelCopy(source: ReadableByteChannel, target: WritableByteChannel, bufferSize: Int, copyListener: Option[CopyListener] = None): Unit = {
    val buffer = ByteBuffer.allocate(bufferSize)
    while (source.read(buffer) != -1) {
      buffer.flip()
      val count = target.write(buffer)
      val continue = copyListener.forall(_.bytesWritten(count))
      buffer.compact()

      if (!continue)
        throw new AbortOperationException
    }

    buffer.flip()
    while(buffer.hasRemaining) {
      val count = target.write(buffer)
      val continue = copyListener.forall(_.bytesWritten(count))

      if (!continue)
        throw new AbortOperationException
    }
  }
}
