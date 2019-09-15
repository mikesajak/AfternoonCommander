package org.mikesajak.commander.util

import java.nio.ByteBuffer
import java.nio.channels.{ReadableByteChannel, WritableByteChannel}

import org.mikesajak.commander.task.CancelledException

object IO {
  trait CopyListener {
    def notifyBytesWritten(size: Long): Boolean
  }

  def channelCopy(source: ReadableByteChannel, target: WritableByteChannel, bufferSize: Int, copyListener: CopyListener): Unit =
    channelCopy(source, target, bufferSize, Some(copyListener))

  def channelCopy(source: ReadableByteChannel, target: WritableByteChannel, bufferSize: Int, copyListener: Option[CopyListener] = None): Unit = {
    val buffer = ByteBuffer.allocate(bufferSize)
    var count = 0L
    while (source.read(buffer) != -1) {
      buffer.flip()
      count += target.write(buffer)
      val cancelled = copyListener.forall(_.notifyBytesWritten(count))
      buffer.compact()

      if (cancelled)
        throw new CancelledException
    }

    buffer.flip()
    while(buffer.hasRemaining) {
      count += target.write(buffer)
      val continue = copyListener.forall(_.notifyBytesWritten(count))

      if (!continue)
        throw new CancelledException
    }
  }
}
