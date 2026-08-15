package eivydas.senkus.logcruiser.index

import java.io.Closeable
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.StandardOpenOption

class OffsetLineReader(file: File) : Closeable {
  private val channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)
  private val fileLength = file.length().toInt()

  fun readLine(index: LogFileIndex, lineIndex: Int): String {
    if (lineIndex < 0 || lineIndex >= index.lineCount) {
      return ""
    }

    val startOffset = index.offset(lineIndex)
    val endOffset =
        if (lineIndex + 1 < index.lineCount) {
          index.offset(lineIndex + 1)
        } else {
          fileLength
        }
    val length = endOffset - startOffset
    if (length <= 0) {
      return ""
    }

    val buffer = ByteBuffer.allocate(length)
    channel.read(buffer, startOffset.toLong())
    buffer.flip()
    var text = StandardCharsets.UTF_8.decode(buffer).toString()

    text =
        when {
          text.endsWith("\r\n") -> text.dropLast(2)
          text.endsWith("\n") -> text.dropLast(1)
          text.endsWith("\r") -> text.dropLast(1)
          else -> text
        }

    return text
  }

  override fun close() {
    channel.close()
  }
}
