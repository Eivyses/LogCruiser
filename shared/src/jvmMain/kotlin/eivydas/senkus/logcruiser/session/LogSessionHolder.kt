package eivydas.senkus.logcruiser.session

import eivydas.senkus.logcruiser.filter.FilterEngine
import eivydas.senkus.logcruiser.index.FilteredLogFileIndex
import eivydas.senkus.logcruiser.index.IndexingProgress
import eivydas.senkus.logcruiser.index.LogFileIndex
import eivydas.senkus.logcruiser.index.OffsetLineReader
import eivydas.senkus.logcruiser.model.FilterDef
import eivydas.senkus.logcruiser.model.FilterKind
import eivydas.senkus.logcruiser.model.FilterType
import eivydas.senkus.logcruiser.model.IncludeMatchMode
import java.io.File
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed class ViewerState {
  data object Idle : ViewerState()

  data class Indexing(val progress: IndexingProgress) : ViewerState()

  data class Ready(
      val filteredIndex: FilteredLogFileIndex,
      val reader: OffsetLineReader,
      val isFiltering: Boolean,
  ) : ViewerState()
}

class LogSessionHolder(coroutineContext: CoroutineContext = Dispatchers.Default) {
  private val scope = CoroutineScope(SupervisorJob() + coroutineContext)
  // A replacement session must wait until the previous reader has stopped being used and closed.
  private val lifecycleMutex = Mutex()
  private val _state = MutableStateFlow<ViewerState>(ViewerState.Idle)
  private val _filters = MutableStateFlow(emptyList<FilterDef>())
  private val _includeMatchMode = MutableStateFlow(IncludeMatchMode.Any)

  private var sessionJob: Job? = null
  private var session: Session? = null

  val state: StateFlow<ViewerState> = _state.asStateFlow()
  val filters: StateFlow<List<FilterDef>> = _filters.asStateFlow()
  val includeMatchMode: StateFlow<IncludeMatchMode> = _includeMatchMode.asStateFlow()

  fun openFile(file: File) {
    // Remove the old reader from the UI before cancellation can close it.
    _state.value = ViewerState.Idle
    sessionJob?.cancel()
    sessionJob = scope.launch {
      lifecycleMutex.withLock { runSession(file) }
    }
  }

  fun cancelIndexing() {
    sessionJob?.cancel()
    sessionJob = null
    session = null
    _state.value = ViewerState.Idle
  }

  fun addFilter(kind: FilterKind, value: String) {
    val trimmedValue = value.trim()
    val filter =
        FilterDef(
            id = UUID.randomUUID().toString(),
            kind = kind,
            type = FilterType.Substring,
            value = trimmedValue,
        )
    _filters.update { it + filter }
  }

  fun toggleFilter(id: String) {
    _filters.update { filters ->
      filters.map { filter ->
        if (filter.id == id) filter.copy(enabled = !filter.enabled) else filter
      }
    }
  }

  fun deleteFilter(id: String) {
    _filters.update { filters -> filters.filterNot { it.id == id } }
  }

  fun setIncludeMatchMode(mode: IncludeMatchMode) {
    _includeMatchMode.value = mode
  }

  fun dispose() {
    _state.value = ViewerState.Idle
    sessionJob?.cancel()
    scope.cancel()
    session = null
  }

  private suspend fun runSession(file: File) {
    var reader: OffsetLineReader? = null
    try {
      val index = LogFileIndex(file)
      coroutineScope {
        val progressJob = launch {
          index.progress.collect { progress ->
            _state.value = ViewerState.Indexing(progress)
          }
        }
        index.build()
        progressJob.cancel()
        ensureActive()
      }

      // The reader stays open for the whole session, including every filter re-evaluation.
      val openedReader = OffsetLineReader(file)
      reader = openedReader
      val currentSession = Session(index, openedReader)
      session = currentSession
      _state.value =
          ViewerState.Ready(
              filteredIndex = FilteredLogFileIndex.all(index),
              reader = currentSession.reader,
              isFiltering = false,
          )

      combine(_filters, _includeMatchMode) { filters, includeMatchMode ->
            FilterRequest(filters, includeMatchMode)
          }
          .collectLatest { request ->
            if (request.filters.none { it.enabled }) {
              _state.value =
                  ViewerState.Ready(
                      filteredIndex = FilteredLogFileIndex.all(currentSession.index),
                      reader = currentSession.reader,
                      isFiltering = false,
                  )
              return@collectLatest
            }

            _state.update { state ->
              if (state is ViewerState.Ready && state.reader === currentSession.reader) {
                state.copy(isFiltering = true)
              } else {
                state
              }
            }

            val filteredIndex =
                FilterEngine.filter(
                    index = currentSession.index,
                    reader = currentSession.reader,
                    filters = request.filters,
                    includeMatchMode = request.includeMatchMode,
                )
            if (session === currentSession) {
              _state.value =
                  ViewerState.Ready(
                      filteredIndex = filteredIndex,
                      reader = currentSession.reader,
                      isFiltering = false,
                  )
            }
          }
    } finally {
      if (session?.reader === reader) {
        session = null
      }
      reader?.close()
    }
  }

  private data class Session(val index: LogFileIndex, val reader: OffsetLineReader)

  private data class FilterRequest(
      val filters: List<FilterDef>,
      val includeMatchMode: IncludeMatchMode,
  )
}
