package eivydas.senkus.logcruiser.index

/**
 * A filtered view over the source file index.
 *
 * Positions in the filtered view do not necessarily match positions in the source file. For
 * example, filtered position 0 may represent source line 42. The mapping keeps the source line
 * available for reading and for preserving the original line number in the viewport.
 *
 * A null mapping represents an unfiltered view, where both positions are identical. This avoids
 * allocating another large identity array.
 */
class FilteredLogFileIndex(
    val sourceIndex: LogFileIndex,
    internal val visibleSourceLineIndexes: IntArray?,
) {
  val lineCount: Int
    get() = visibleSourceLineIndexes?.size ?: sourceIndex.lineCount

  fun sourceLineIndexAt(filteredLineIndex: Int): Int =
      visibleSourceLineIndexes?.get(filteredLineIndex) ?: filteredLineIndex

  companion object {
    /** Creates an unfiltered view without allocating a second array of line indexes. */
    fun all(sourceIndex: LogFileIndex): FilteredLogFileIndex =
        FilteredLogFileIndex(sourceIndex = sourceIndex, visibleSourceLineIndexes = null)
  }
}
