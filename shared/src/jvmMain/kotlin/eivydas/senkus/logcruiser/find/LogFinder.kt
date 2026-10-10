package eivydas.senkus.logcruiser.find

import eivydas.senkus.logcruiser.index.FilteredLogFileIndex
import eivydas.senkus.logcruiser.index.OffsetLineReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

data class LogMatch(
    val filteredLineIndex: Int,
    val sourceLineIndex: Int,
    val start: Int,
    val end: Int,
)

object LogFinder {
  suspend fun scan(
      index: FilteredLogFileIndex,
      reader: OffsetLineReader,
      query: String,
      onChunk: suspend (List<LogMatch>) -> Unit,
  ) =
      withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext

        val matches = mutableListOf<LogMatch>()
        for (filteredLineIndex in 0 until index.lineCount) {
          coroutineContext.ensureActive()
          val sourceLineIndex = index.sourceLineIndexAt(filteredLineIndex)
          val line = reader.readLine(index.sourceIndex, sourceLineIndex)
          var from = 0
          while (true) {
            val start = line.indexOf(query, from, ignoreCase = true)
            if (start < 0) break
            matches.add(
                LogMatch(
                    filteredLineIndex = filteredLineIndex,
                    sourceLineIndex = sourceLineIndex,
                    start = start,
                    end = start + query.length,
                )
            )
            from = start + query.length
            if (matches.size == 100) {
              onChunk(matches.toList())
              matches.clear()
            }
          }

          if ((filteredLineIndex + 1) % 500 == 0 && matches.isNotEmpty()) {
            onChunk(matches.toList())
            matches.clear()
          }
        }

        if (matches.isNotEmpty()) onChunk(matches)
      }
}
