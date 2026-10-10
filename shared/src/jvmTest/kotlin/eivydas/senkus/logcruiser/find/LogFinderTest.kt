package eivydas.senkus.logcruiser.find

import eivydas.senkus.logcruiser.index.FilteredLogFileIndex
import eivydas.senkus.logcruiser.index.LogFileIndex
import eivydas.senkus.logcruiser.index.OffsetLineReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class LogFinderTest {
  @Test
  fun `finds case-insensitive occurrences and reports positions`() = runBlocking {
    val file = logFile("error found\nnothing here\nprefix ErRoR suffix\n")
    val sourceIndex = LogFileIndex(file).also { it.build() }
    val matches = mutableListOf<LogMatch>()

    OffsetLineReader(file).use { reader ->
      LogFinder.scan(FilteredLogFileIndex.all(sourceIndex), reader, "ERROR") {
        matches.addAll(it)
      }
    }

    assertEquals(
        listOf(
            LogMatch(filteredLineIndex = 0, sourceLineIndex = 0, start = 0, end = 5),
            LogMatch(filteredLineIndex = 2, sourceLineIndex = 2, start = 7, end = 12),
        ),
        matches,
    )
  }

  @Test
  fun `searches only visible lines and reports filtered and source positions`() = runBlocking {
    val file = logFile("error hidden\nfirst ERROR visible\nno match\nvisible error\n")
    val sourceIndex = LogFileIndex(file).also { it.build() }
    val filteredIndex = FilteredLogFileIndex(sourceIndex, intArrayOf(1, 3))
    val matches = mutableListOf<LogMatch>()

    OffsetLineReader(file).use { reader ->
      LogFinder.scan(filteredIndex, reader, "error") { matches.addAll(it) }
    }

    assertEquals(
        listOf(
            LogMatch(filteredLineIndex = 0, sourceLineIndex = 1, start = 6, end = 11),
            LogMatch(filteredLineIndex = 1, sourceLineIndex = 3, start = 8, end = 13),
        ),
        matches,
    )
  }

  @Test
  fun `finds non-overlapping occurrences on one line`() = runBlocking {
    val file = logFile("aaaa\n")
    val sourceIndex = LogFileIndex(file).also { it.build() }
    val matches = mutableListOf<LogMatch>()

    OffsetLineReader(file).use { reader ->
      LogFinder.scan(FilteredLogFileIndex.all(sourceIndex), reader, "aa") { matches.addAll(it) }
    }

    assertEquals(
        listOf(
            LogMatch(filteredLineIndex = 0, sourceLineIndex = 0, start = 0, end = 2),
            LogMatch(filteredLineIndex = 0, sourceLineIndex = 0, start = 2, end = 4),
        ),
        matches,
    )
  }

  @Test
  fun `blank query does not emit chunks`() = runBlocking {
    val file = logFile("some log line\n")
    val sourceIndex = LogFileIndex(file).also { it.build() }
    var emitted = false

    OffsetLineReader(file).use { reader ->
      LogFinder.scan(FilteredLogFileIndex.all(sourceIndex), reader, " \t ") { emitted = true }
    }

    assertEquals(false, emitted)
  }

  private fun logFile(contents: String): File =
      File.createTempFile("logcruiser-find", ".txt").apply {
        deleteOnExit()
        writeText(contents, Charsets.UTF_8)
      }
}
