package eivydas.senkus.logcruiser.session

import eivydas.senkus.logcruiser.model.FilterKind
import eivydas.senkus.logcruiser.model.IncludeMatchMode
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class LogSessionHolderTest {
  @Test
  fun `add filter trims value`() = runBlocking {
    val holder = LogSessionHolder()
    try {
      holder.addFilter(FilterKind.Include, "  error  ")

      assertEquals("error", holder.filters.value.single().value)
    } finally {
      holder.dispose()
    }
  }

  @Test
  fun `blank filter is rejected`() =
      runBlocking<Unit> {
        val holder = LogSessionHolder()
        try {
          assertFailsWith<IllegalArgumentException> {
            holder.addFilter(FilterKind.Include, "   ")
          }
        } finally {
          holder.dispose()
        }
      }

  @Test
  fun `opened file uses filtered projection`() = runBlocking {
    val file = createTempFile()
    val holder = LogSessionHolder()
    try {
      holder.openFile(file)
      val ready =
          withTimeout(5.seconds) {
            holder.state.filterIsInstance<ViewerState.Ready>().first()
          }
      assertEquals(2, ready.filteredIndex.lineCount)

      holder.addFilter(FilterKind.Include, "error")
      val filtered =
          withTimeout(5.seconds) {
            holder.state.filterIsInstance<ViewerState.Ready>().first {
              !it.isFiltering && it.filteredIndex.lineCount == 1
            }
          }
      assertTrue(!filtered.isFiltering)
      assertEquals(1, filtered.filteredIndex.sourceLineIndexAt(0))
    } finally {
      holder.dispose()
    }
  }

  @Test
  fun `opening another file supersedes the previous session`() = runBlocking {
    val firstFile = createTempFile("A\n".repeat(100_000))
    val secondFile = createTempFile("B\n")
    val holder = LogSessionHolder()
    try {
      holder.openFile(firstFile)
      holder.openFile(secondFile)

      val ready =
          withTimeout(5.seconds) {
            holder.state.filterIsInstance<ViewerState.Ready>().first {
              it.filteredIndex.sourceIndex.lineCount == 1
            }
          }
      assertEquals(1, ready.filteredIndex.lineCount)
    } finally {
      holder.dispose()
    }
  }

  @Test
  fun `changing include match mode recomputes the projection`() = runBlocking {
    val file = createTempFile("ERROR failed timeout\nERROR failed\nERROR timeout\nINFO ready\n")
    val holder = LogSessionHolder()
    try {
      holder.openFile(file)
      withTimeout(5.seconds) {
        holder.state.filterIsInstance<ViewerState.Ready>().first()
      }
      holder.addFilter(FilterKind.Include, "error")
      holder.addFilter(FilterKind.Include, "timeout")

      val anyReady =
          withTimeout(5.seconds) {
            holder.state.filterIsInstance<ViewerState.Ready>().first {
              !it.isFiltering && it.filteredIndex.lineCount == 3
            }
          }
      assertEquals(3, anyReady.filteredIndex.lineCount)

      holder.setIncludeMatchMode(IncludeMatchMode.All)
      val allReady =
          withTimeout(5.seconds) {
            holder.state.filterIsInstance<ViewerState.Ready>().first {
              it.filteredIndex.lineCount == 2
            }
          }
      assertTrue(!allReady.isFiltering)
      assertEquals(2, allReady.filteredIndex.lineCount)
    } finally {
      holder.dispose()
    }
  }

  private fun createTempFile(content: String = "INFO ready\nERROR failed\n"): File {
    val file = File.createTempFile("logcruiser-session", ".txt")
    file.deleteOnExit()
    file.writeText(content, Charsets.UTF_8)
    return file
  }
}
