package eivydas.senkus.logcruiser.index

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

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
      val fileOffsets = IntArrayList(initialCapacity = 1024)
      if (totalBytes > 0) {
        fileOffsets.add(0)
      }

      // allocateDirect allocates memory outside the JVM heap skipping intermediate heap
      // allocations. Performance difference is probably negligible but let's keep it.
      val buffer = ByteBuffer.allocateDirect(64 * 1024)
      var filePosition: Long = 0

      FileChannel.open(file.toPath(), StandardOpenOption.READ).use { channel ->
        while (channel.read(buffer) > 0) {
          buffer.flip()
          val limit = buffer.limit()

          for (i in 0 until limit) {
            // Newline byte check ('\n' = 0x0A)
            if (buffer.get(i) == 0x0A.toByte()) {
              val nextLineStart = filePosition + i + 1
              if (nextLineStart < totalBytes) {
                fileOffsets.add(nextLineStart.toInt())
              }
            }
          }

          filePosition += limit
          buffer.clear()
          _progress.value = IndexingProgress(filePosition, totalBytes, done = false)
          ensureActive()
        }
      }

      offsets = fileOffsets.toArray()
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
