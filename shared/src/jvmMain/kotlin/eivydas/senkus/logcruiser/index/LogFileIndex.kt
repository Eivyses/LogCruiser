package eivydas.senkus.logcruiser.index

import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

data class IndexingProgress(val bytesRead: Long, val totalBytes: Long, val done: Boolean)

class LogFileIndex(private val file: File) {
  private val _progress =
      MutableStateFlow(IndexingProgress(bytesRead = 0, totalBytes = file.length(), done = false))
  val progress: StateFlow<IndexingProgress> = _progress.asStateFlow()

  private var offsets: IntArray = IntArray(0)
  val offsetsArray: IntArray
    get() = offsets

  val lineCount: Int
    get() = offsets.size

  suspend fun build() {
    withContext(Dispatchers.IO) {
      val totalBytes = file.length()
      val lineStarts = IntArrayList(initialCapacity = 1024)
      if (totalBytes > 0) {
        lineStarts.add(0)
      }

      val buffer = ByteArray(64 * 1024)
      var bytesRead: Long = 0

      file.inputStream().use { stream: InputStream ->
        while (isActive) {
          val read = stream.read(buffer)
          if (read <= 0) break

          for (i in 0 until read) {
            if (buffer[i] == '\n'.code.toByte()) {
              val nextLineStart = bytesRead + i + 1
              if (nextLineStart < totalBytes) {
                lineStarts.add(nextLineStart.toInt())
              }
            }
          }

          bytesRead += read
          _progress.value = IndexingProgress(bytesRead, totalBytes, done = false)
          ensureActive()
        }
      }

      offsets = lineStarts.toArray()
      _progress.value = IndexingProgress(totalBytes, totalBytes, done = true)
    }
  }

  fun offset(lineIndex: Int): Int = offsets[lineIndex]

  private class IntArrayList(initialCapacity: Int = 16) {
    private var data: IntArray = IntArray(initialCapacity)
    var size: Int = 0
      private set

    fun add(value: Int) {
      if (size == data.size) {
        data = data.copyOf(data.size * 2)
      }
      data[size] = value
      size++
    }

    fun toArray(): IntArray = data.copyOf(size)
  }
}
