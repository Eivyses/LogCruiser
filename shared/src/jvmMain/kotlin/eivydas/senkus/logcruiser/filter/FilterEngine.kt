package eivydas.senkus.logcruiser.filter

import eivydas.senkus.logcruiser.index.FilteredLogFileIndex
import eivydas.senkus.logcruiser.index.IntArrayBuilder
import eivydas.senkus.logcruiser.index.LogFileIndex
import eivydas.senkus.logcruiser.index.OffsetLineReader
import eivydas.senkus.logcruiser.model.FilterDef
import eivydas.senkus.logcruiser.model.FilterKind
import eivydas.senkus.logcruiser.model.FilterType
import eivydas.senkus.logcruiser.model.IncludeMatchMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

object FilterEngine {
  suspend fun filter(
      index: LogFileIndex,
      reader: OffsetLineReader,
      filters: List<FilterDef>,
      includeMatchMode: IncludeMatchMode = IncludeMatchMode.Any,
  ): FilteredLogFileIndex =
      withContext(Dispatchers.IO) {
        val activeFilters = filters.filter { it.enabled }
        if (activeFilters.isEmpty()) {
          return@withContext FilteredLogFileIndex.all(index)
        }

        // Excludes always use OR semantics; an active exclude matching the line removes it.
        val includeFilters = activeFilters.filter { it.kind == FilterKind.Include }
        val excludeFilters = activeFilters.filter { it.kind == FilterKind.Exclude }
        val visibleSourceLineIndexes = IntArrayBuilder()

        for (sourceLineIndex in 0 until index.lineCount) {
          ensureActive()
          val line = reader.readLine(index, sourceLineIndex)
          val included = includeFilters.matches(line, includeMatchMode)
          val excluded = excludeFilters.any { filter -> matches(filter, line) }

          if (included && !excluded) {
            visibleSourceLineIndexes.add(sourceLineIndex)
          }
        }

        FilteredLogFileIndex(
            sourceIndex = index,
            visibleSourceLineIndexes = visibleSourceLineIndexes.toArray(),
        )
      }

  private fun matches(filter: FilterDef, line: String): Boolean =
      when (filter.type) {
        FilterType.Substring -> line.contains(filter.value, ignoreCase = !filter.caseSensitive)
      }

  private fun List<FilterDef>.matches(line: String, mode: IncludeMatchMode): Boolean =
      when {
        isEmpty() -> true
        mode == IncludeMatchMode.Any -> any { filter -> matches(filter, line) }
        else -> all { filter -> matches(filter, line) }
      }
}
