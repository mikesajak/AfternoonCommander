package org.mikesajak.commander.util

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer
import java.nio.channels.{Channels, ReadableByteChannel, WritableByteChannel}

import org.mikesajak.commander.util.Utils.using

object IO {
  trait CopyListener {
    def notifyBytesWritten(size: Long)
  }

  def bufferedCopy(inStream: InputStream, outStream: OutputStream, bufferSize: Int, copyListener: IO.CopyListener): Unit =
    bufferedCopy(Channels.newChannel(inStream), Channels.newChannel(outStream), bufferSize, Some(copyListener))

  def bufferedCopy(inStream: InputStream, outStream: OutputStream, bufferSize: Int, copyListener: Option[IO.CopyListener]): Unit =
    bufferedCopy(Channels.newChannel(inStream), Channels.newChannel(outStream), bufferSize, copyListener)

  def bufferedCopy(source: ReadableByteChannel, target: WritableByteChannel, bufferSize: Int, copyListener: CopyListener): Unit =
    bufferedCopy(source, target, bufferSize, Some(copyListener))

  def bufferedCopy(source: ReadableByteChannel, target: WritableByteChannel, bufferSize: Int, copyListener: Option[CopyListener] = None): Unit =
    using(source, target) { (inChannel, outChannel) =>
      val buffer = ByteBuffer.allocate(bufferSize)
      var count = 0L
      while (inChannel.read(buffer) != -1) {
        buffer.flip()
        count += outChannel.write(buffer)
        copyListener.foreach(_.notifyBytesWritten(count))
        buffer.compact()
      }

      buffer.flip()
      while (buffer.hasRemaining) {
        count += outChannel.write(buffer)
        copyListener.foreach(_.notifyBytesWritten(count))
      }
    }
}
