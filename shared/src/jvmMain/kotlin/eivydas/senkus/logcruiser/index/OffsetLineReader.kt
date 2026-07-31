package eivydas.senkus.logcruiser.index

import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile

class OffsetLineReader(file: File) : Closeable {
  private val raf = RandomAccessFile(file, "r")

  fun readLine(offsets: IntArray, lineIndex: Int): String {
    if (lineIndex < 0 || lineIndex >= offsets.size) {
      return ""
    }

    val startOffset = offsets[lineIndex]
    val endOffset =
        if (lineIndex + 1 < offsets.size) offsets[lineIndex + 1] else raf.length().toInt()
    val length = endOffset - startOffset
    if (length <= 0) {
      return ""
    }

    val bytes = ByteArray(length)
    raf.seek(startOffset.toLong())
    val count = raf.read(bytes)
    if (count <= 0) {
      return ""
    }

    var end = count
    while (
        end > 0 && (bytes[end - 1] == '\n'.code.toByte() || bytes[end - 1] == '\r'.code.toByte())
    ) {
      end--
    }

    return String(bytes, 0, end, Charsets.UTF_8)
  }

  override fun close() {
    raf.close()
  }
}
