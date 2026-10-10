package eivydas.senkus.logcruiser.ui.find

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eivydas.senkus.logcruiser.find.LogFinder
import eivydas.senkus.logcruiser.find.LogMatch
import eivydas.senkus.logcruiser.index.FilteredLogFileIndex
import eivydas.senkus.logcruiser.index.OffsetLineReader
import eivydas.senkus.logcruiser.session.ViewerState
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds

class FindBarController(private val scope: CoroutineScope) {
  var isOpen by mutableStateOf(false)
    private set

  var focusRequestId by mutableIntStateOf(0)
    private set

  var query by mutableStateOf("")
    private set

  var matches by mutableStateOf<List<LogMatch>?>(null)
    private set

  var currentIndex by mutableStateOf(-1)
    private set

  var isScanning by mutableStateOf(false)
    private set

  private var scanJob: Job? = null
  private var scanGeneration = 0
  private var target: Pair<FilteredLogFileIndex, OffsetLineReader>? = null

  fun open() {
    if (!isOpen) {
      isOpen = true
      restartScan()
    }
    focusRequestId++
  }

  fun close() {
    isOpen = false
    scanGeneration++
    scanJob?.cancel()
    scanJob = null
    matches = null
    currentIndex = -1
    isScanning = false
  }

  fun onQueryChange(value: String) {
    query = value
    restartScan()
  }

  fun next() {
    val currentMatches = matches?.takeIf { it.isNotEmpty() } ?: return
    currentIndex = if (currentIndex == -1) 0 else (currentIndex + 1) % currentMatches.size
  }

  fun previous() {
    val currentMatches = matches?.takeIf { it.isNotEmpty() } ?: return
    currentIndex = if (currentIndex == -1) 0 else (currentIndex - 1).mod(currentMatches.size)
  }

  fun onViewportChanged(ready: ViewerState.Ready?) {
    if (ready == null) {
      scanGeneration++
      scanJob?.cancel()
      scanJob = null
      target = null
      matches = null
      currentIndex = -1
      isOpen = false
      isScanning = false
      return
    }

    val oldTarget = target
    if (
        oldTarget == null ||
            ready.filteredIndex !== oldTarget.first ||
            ready.reader !== oldTarget.second
    ) {
      target = ready.filteredIndex to ready.reader
      restartScan()
    }
  }

  private fun restartScan() {
    scanGeneration++
    val generation = scanGeneration
    scanJob?.cancel()
    scanJob = null
    if (!isOpen || query.isBlank()) {
      matches = null
      currentIndex = -1
      isScanning = false
      return
    }

    matches = emptyList()
    currentIndex = -1
    isScanning = true
    scanJob = scope.launch {
      try {
        delay(150.milliseconds)
        val (filteredIndex, reader) = target ?: return@launch
        LogFinder.scan(filteredIndex, reader, query.trim()) { chunk ->
          withContext(Dispatchers.Main.immediate) {
            if (generation == scanGeneration) {
              matches = (matches ?: emptyList()) + chunk
              if (currentIndex == -1 && !matches.isNullOrEmpty()) currentIndex = 0
            }
          }
        }
      } finally {
        if (generation == scanGeneration) isScanning = false
      }
    }
  }
}
