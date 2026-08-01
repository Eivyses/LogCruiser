package eivydas.senkus.logcruiser.index

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class OffsetLineReaderTest {

  @Test
  fun `test read line by index`() {
    val file = createTempFile("line1\nline2\nline3\n")
    val index = buildIndex(file)
    OffsetLineReader(file).use { reader ->
      assertEquals("line1", reader.readLine(index.offsetsArray, 0))
      assertEquals("line2", reader.readLine(index.offsetsArray, 1))
      assertEquals("line3", reader.readLine(index.offsetsArray, 2))
    }
  }

  @Test
  fun `test read out of bounds`() {
    val file = createTempFile("line1\nline2\n")
    val index = buildIndex(file)
    OffsetLineReader(file).use { reader ->
      assertEquals("", reader.readLine(index.offsetsArray, -1))
      assertEquals("", reader.readLine(index.offsetsArray, 2))
      assertEquals("", reader.readLine(index.offsetsArray, 100))
    }
  }

  @Test
  fun `test read line with CRLF`() {
    val file = createTempFile("line1\r\nline2\r\n")
    val index = buildIndex(file)
    OffsetLineReader(file).use { reader ->
      assertEquals("line1", reader.readLine(index.offsetsArray, 0))
      assertEquals("line2", reader.readLine(index.offsetsArray, 1))
    }
  }

  @Test
  fun `test read empty lines`() {
    val file = createTempFile("line1\n\nline3\n")
    val index = buildIndex(file)
    OffsetLineReader(file).use { reader ->
      assertEquals("line1", reader.readLine(index.offsetsArray, 0))
      assertEquals("", reader.readLine(index.offsetsArray, 1))
      assertEquals("line3", reader.readLine(index.offsetsArray, 2))
    }
  }

  @Test
  fun `test read standalone trailing CR`() {
    val file = createTempFile("line1\r\nline2\r")
    val index = buildIndex(file)
    OffsetLineReader(file).use { reader ->
      assertEquals("line1", reader.readLine(index.offsetsArray, 0))
      assertEquals("line2", reader.readLine(index.offsetsArray, 1))
    }
  }

  @Test
  fun `test read internal CR preserved trailing stripped`() {
    val file = createTempFile("a\r\nb\rc\r\nd\r")
    val index = buildIndex(file)
    OffsetLineReader(file).use { reader ->
      assertEquals("a", reader.readLine(index.offsetsArray, 0))
      assertEquals("b\rc", reader.readLine(index.offsetsArray, 1))
      assertEquals("d", reader.readLine(index.offsetsArray, 2))
    }
  }

  @Test
  fun `test read first and last line`() {
    val file = createTempFile("first\nmiddle\nlast")
    val index = buildIndex(file)
    OffsetLineReader(file).use { reader ->
      assertEquals("first", reader.readLine(index.offsetsArray, 0))
      assertEquals("middle", reader.readLine(index.offsetsArray, 1))
      assertEquals("last", reader.readLine(index.offsetsArray, 2))
    }
  }

  @Test
  fun `test read single line no newline`() {
    val file = createTempFile("only line")
    val index = buildIndex(file)
    OffsetLineReader(file).use { reader ->
      assertEquals("only line", reader.readLine(index.offsetsArray, 0))
    }
  }

  @Test
  fun `test read UTF8 multibyte characters`() {
    val file = createTempFile("café\n日本語\n🏳️‍🌈\n")
    val index = buildIndex(file)
    OffsetLineReader(file).use { reader ->
      assertEquals("café", reader.readLine(index.offsetsArray, 0))
      assertEquals("日本語", reader.readLine(index.offsetsArray, 1))
      assertEquals("🏳️‍🌈", reader.readLine(index.offsetsArray, 2))
    }
  }

  @Test
  fun `test read long lines`() {
    val longText = "A".repeat(10_000)
    val file = createTempFile("$longText\n$longText\n")
    val index = buildIndex(file)
    OffsetLineReader(file).use { reader ->
      assertEquals(longText, reader.readLine(index.offsetsArray, 0))
      assertEquals(longText, reader.readLine(index.offsetsArray, 1))
    }
  }

  private fun buildIndex(file: File): LogFileIndex {
    val index = LogFileIndex(file)
    runBlocking { index.build() }
    return index
  }

  private fun createTempFile(content: String): File {
    val file = File.createTempFile("logcruiser-test", ".txt")
    file.deleteOnExit()
    file.writeText(content, Charsets.UTF_8)
    return file
  }
}
