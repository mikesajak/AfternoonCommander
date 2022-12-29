package org.mikesajak.commander.util

import com.glaforge.i18n.io.CharsetToolkit
import org.mikesajak.commander.fs.VFile

import java.io.InputStream
import java.nio.charset.{Charset, StandardCharsets}
import scala.util.Using

object FileUtils {
  def detectCharset(vfile: VFile, sampleBufSize: Int = 4096, defaultCharset: Charset = StandardCharsets.UTF_8): Charset =
    detectCharset(vfile.inStream, sampleBufSize, defaultCharset)

  def detectCharset(inStream: InputStream, sampleBufSize: Int, defaultCharset: Charset): Charset = {
    Using.resource(inStream) { stream =>
      val buffer = new Array[Byte](sampleBufSize)
      stream.read(buffer)
      val toolkit = new CharsetToolkit(buffer)
      toolkit.setDefaultCharset(defaultCharset)
      toolkit.guessEncoding()
    }
  }

}
