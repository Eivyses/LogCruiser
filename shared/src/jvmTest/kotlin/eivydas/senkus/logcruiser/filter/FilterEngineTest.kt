package eivydas.senkus.logcruiser.filter

import eivydas.senkus.logcruiser.index.FilteredLogFileIndex
import eivydas.senkus.logcruiser.index.LogFileIndex
import eivydas.senkus.logcruiser.index.OffsetLineReader
import eivydas.senkus.logcruiser.model.FilterDef
import eivydas.senkus.logcruiser.model.FilterKind
import eivydas.senkus.logcruiser.model.FilterType
import eivydas.senkus.logcruiser.model.IncludeMatchMode
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class FilterEngineTest {
  @Test
  fun `contains filters use OR composition`() = runBlocking {
    val projection =
        filter(
            content = "INFO ready\nERROR failed\nDEBUG idle\n",
            filters =
                listOf(
                    filter(FilterKind.Include, "ready"),
                    filter(FilterKind.Include, "failed"),
                ),
        )

    assertProjection(projection, intArrayOf(0, 1))
  }

  @Test
  fun `contains filters use AND composition when requested`() = runBlocking {
    val projection =
        filter(
            content = "ERROR timeout\nERROR failed\nWARN timeout\n",
            filters =
                listOf(
                    filter(FilterKind.Include, "error"),
                    filter(FilterKind.Include, "timeout"),
                ),
            includeMatchMode = IncludeMatchMode.All,
        )

    assertProjection(projection, intArrayOf(0))
  }

  @Test
  fun `not contains filters exclude matching lines`() = runBlocking {
    val projection =
        filter(
            content = "INFO ready\nERROR failed\nDEBUG idle\n",
            filters = listOf(filter(FilterKind.Exclude, "error")),
        )

    assertProjection(projection, intArrayOf(0, 2))
  }

  @Test
  fun `excludes use OR composition regardless of include mode`() = runBlocking {
    val projection =
        filter(
            content = "normal\nhealth\nmetrics\n",
            filters =
                listOf(
                    filter(FilterKind.Exclude, "health"),
                    filter(FilterKind.Exclude, "metrics"),
                ),
            includeMatchMode = IncludeMatchMode.All,
        )

    assertProjection(projection, intArrayOf(0))
  }

  @Test
  fun `includes and excludes are composed together`() = runBlocking {
    val projection =
        filter(
            content = "INFO ready\nERROR failed\nERROR retrying\nDEBUG idle\n",
            filters =
                listOf(
                    filter(FilterKind.Include, "error"),
                    filter(FilterKind.Exclude, "retrying"),
                ),
        )

    assertProjection(projection, intArrayOf(1))
  }

  @Test
  fun `disabled filters do not affect projection`() = runBlocking {
    val projection =
        filter(
            content = "INFO ready\nERROR failed\n",
            filters = listOf(filter(FilterKind.Exclude, "error").copy(enabled = false)),
        )

    assertProjection(projection, intArrayOf(0, 1))
  }

  @Test
  fun `empty active filter set returns every raw line`() = runBlocking {
    val projection = filter(content = "one\ntwo\nthree\n", filters = emptyList())

    assertProjection(projection, intArrayOf(0, 1, 2))
  }

  @Test
  fun `substring matching is case insensitive by default`() = runBlocking {
    val projection =
        filter(
            content = "Error occurred\ninfo ready\n",
            filters = listOf(filter(FilterKind.Include, "error")),
        )

    assertProjection(projection, intArrayOf(0))
  }

  @Test
  fun `case sensitive filters only match exact case`() = runBlocking {
    val projection =
        filter(
            content = "Error occurred\nerror occurred\n",
            filters = listOf(filter(FilterKind.Include, "error").copy(caseSensitive = true)),
        )

    assertProjection(projection, intArrayOf(1))
  }

  private fun filter(kind: FilterKind, value: String): FilterDef =
      FilterDef(
          id = "$kind-$value",
          kind = kind,
          type = FilterType.Substring,
          value = value,
      )

  private suspend fun filter(
      content: String,
      filters: List<FilterDef>,
      includeMatchMode: IncludeMatchMode = IncludeMatchMode.Any,
  ): FilteredLogFileIndex {
    val file = createTempFile(content)
    val index = LogFileIndex(file)
    index.build()
    OffsetLineReader(file).use { reader ->
      return FilterEngine.filter(index, reader, filters, includeMatchMode)
    }
  }

  private fun assertProjection(projection: FilteredLogFileIndex, expected: IntArray) {
    assertEquals(expected.size, projection.lineCount)
    expected.forEachIndexed { filteredLineIndex, sourceLineIndex ->
      assertEquals(sourceLineIndex, projection.sourceLineIndexAt(filteredLineIndex))
    }
  }

  private fun createTempFile(content: String): File {
    val file = File.createTempFile("logcruiser-filter", ".txt")
    file.deleteOnExit()
    file.writeText(content, Charsets.UTF_8)
    return file
  }
}
