package eivydas.senkus.logcruiser.index

import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class FilteredLogFileIndexTest {
  @Test
  fun `filtered positions map to original line indexes`() = runBlocking {
    val file = File.createTempFile("logcruiser-filtered-index", ".txt")
    file.deleteOnExit()
    file.writeText("one\ntwo\nthree\n", Charsets.UTF_8)

    val sourceIndex = LogFileIndex(file)
    sourceIndex.build()
    val filteredIndex = FilteredLogFileIndex(sourceIndex, intArrayOf(2, 0))

    assertEquals(2, filteredIndex.lineCount)
    assertEquals(2, filteredIndex.sourceLineIndexAt(0))
    assertEquals(0, filteredIndex.sourceLineIndexAt(1))
  }

  @Test
  fun `all projection contains every source line`() = runBlocking {
    val file = File.createTempFile("logcruiser-all-index", ".txt")
    file.deleteOnExit()
    file.writeText("one\ntwo\n", Charsets.UTF_8)

    val sourceIndex = LogFileIndex(file)
    sourceIndex.build()
    val filteredIndex = FilteredLogFileIndex.all(sourceIndex)

    assertEquals(2, filteredIndex.lineCount)
    assertEquals(0, filteredIndex.sourceLineIndexAt(0))
    assertEquals(1, filteredIndex.sourceLineIndexAt(1))
  }
}
