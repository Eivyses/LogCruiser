package eivydas.senkus.logcruiser.index

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

class LogFileIndexTest {

  @Test
  fun `test empty file`() = runBlocking {
    val file = createTempFile("")
    val index = LogFileIndex(file)
    index.build()
    assertEquals(0, index.lineCount)
  }

  @Test
  fun `test single line no trailing newline`() = runBlocking {
    val file = createTempFile("hello world")
    val index = LogFileIndex(file)
    index.build()
    assertEquals(1, index.lineCount)
    assertEquals(0, index.offset(0))
  }

  @Test
  fun `test single line with trailing newline`() = runBlocking {
    val file = createTempFile("hello\n")
    val index = LogFileIndex(file)
    index.build()
    assertEquals(1, index.lineCount)
    assertEquals(0, index.offset(0))
  }

  @Test
  fun `test multiple lines`() = runBlocking {
    val file = createTempFile("line1\nline2\nline3\n")
    val index = LogFileIndex(file)
    index.build()
    assertEquals(3, index.lineCount)
    assertEquals(0, index.offset(0))
    assertEquals(6, index.offset(1))
    assertEquals(12, index.offset(2))
  }

  @Test
  fun `test CRLF line endings`() = runBlocking {
    val file = createTempFile("line1\r\nline2\r\n")
    val index = LogFileIndex(file)
    index.build()
    assertEquals(2, index.lineCount)
    assertEquals(0, index.offset(0))
    assertEquals(7, index.offset(1))
  }

  @Test
  fun `test empty lines`() = runBlocking {
    val file = createTempFile("\n\n\n")
    val index = LogFileIndex(file)
    index.build()
    assertEquals(3, index.lineCount)
    assertEquals(0, index.offset(0))
    assertEquals(1, index.offset(1))
    assertEquals(2, index.offset(2))
  }

  @Test
  fun `test mixed line endings`() = runBlocking {
    val file = createTempFile("a\nb\r\nc\n")
    val index = LogFileIndex(file)
    index.build()
    assertEquals(3, index.lineCount)
    assertEquals(0, index.offset(0))
    assertEquals(2, index.offset(1))
    assertEquals(5, index.offset(2))
  }

  @Test
  fun `test progress`() = runBlocking {
    val file = createTempFile("line1\nline2\nline3\n")
    val index = LogFileIndex(file)
    val initial = index.progress.value
    assertEquals(0L, initial.bytesRead)
    assertEquals(file.length(), initial.totalBytes)
    assertEquals(false, initial.done)

    val progressValues = mutableListOf<IndexingProgress>()
    val collectJob = launch { index.progress.collect { progressValues.add(it) } }
    index.build()
    while (progressValues.lastOrNull()?.done != true) {
      yield()
    }
    collectJob.cancel()

    val final = index.progress.value
    assertEquals(file.length(), final.bytesRead)
    assertEquals(file.length(), final.totalBytes)
    assertEquals(true, final.done)
    assertTrue(progressValues.isNotEmpty())
    assertEquals(true, progressValues.last().done)
  }

  @Test
  fun `test cancellation`() = runBlocking {
    val content = "line content\n".repeat(200_000)
    val file = createTempFile(content)
    val index = LogFileIndex(file)
    val job = launch { index.build() }
    index.progress.first { it.bytesRead > 0 }
    job.cancel()
    job.join()
    assertTrue(job.isCancelled)
    assertEquals(false, index.progress.value.done)
  }

  private fun createTempFile(content: String): File {
    val file = File.createTempFile("logcruiser-test", ".txt")
    file.deleteOnExit()
    file.writeText(content, Charsets.UTF_8)
    return file
  }
}
