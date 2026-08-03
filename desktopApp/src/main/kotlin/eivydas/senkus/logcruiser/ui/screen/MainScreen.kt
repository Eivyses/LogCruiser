package eivydas.senkus.logcruiser.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import eivydas.senkus.logcruiser.index.IndexingProgress
import eivydas.senkus.logcruiser.index.LogFileIndex
import eivydas.senkus.logcruiser.index.OffsetLineReader
import eivydas.senkus.logcruiser.ui.component.LogViewport
import eivydas.senkus.logcruiser.ui.component.MenuBarRow
import eivydas.senkus.logcruiser.ui.component.MenuItem
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal sealed class ViewerState {
  data object Idle : ViewerState()

  data class Indexing(val progress: IndexingProgress) : ViewerState()

  data class Ready(val index: LogFileIndex, val reader: OffsetLineReader) : ViewerState()
}

@Composable
fun MainScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
  var state by remember { mutableStateOf<ViewerState>(ViewerState.Idle) }
  val scope = rememberCoroutineScope()
  val darkLightString = if (isDarkTheme) "light" else "dark"

  val openFile: () -> Unit = {
    val file = showOpenFileDialog()
    if (file != null) {
      scope.launch {
        state = ViewerState.Idle
        val index = LogFileIndex(file)
        val progressJob = launch {
          index.progress.collect { progress ->
            state = ViewerState.Indexing(progress)
          }
        }
        index.build()
        progressJob.cancel()
        state = ViewerState.Ready(index, OffsetLineReader(file))
      }
    }
  }

  val menuItems =
      listOf(
          MenuItem(group = "Files", text = "Open file", onClick = openFile),
          MenuItem(
              group = "Options",
              text = "Change theme to $darkLightString",
              onClick = { onToggleTheme() },
          ),
      )

  val colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()

  MaterialTheme(colorScheme = colorScheme) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize()) {
      Column(modifier = modifier.fillMaxSize()) {
        MenuBarRow(
            menuItems = menuItems,
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
          Row {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
              MainWorkArea(state, scope, onOpenFile = openFile)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MainWorkArea(state: ViewerState, scope: CoroutineScope, onOpenFile: () -> Unit) {
  when (state) {
    ViewerState.Idle -> {
      IdleScreen(
          onOpenFile = onOpenFile,
          modifier = Modifier.fillMaxSize(),
      )
    }

    is ViewerState.Indexing -> {
      IndexingScreen(
          progress = state.progress,
          onCancel = { scope.cancel() },
          modifier = Modifier.fillMaxSize(),
      )
    }

    is ViewerState.Ready -> {
      val index = state.index
      val reader = state.reader

      DisposableEffect(reader) {
        onDispose { reader.close() }
      }

      LogViewport(
          index = index,
          reader = reader,
          modifier = Modifier.fillMaxSize(),
      )
    }
  }
}

private fun showOpenFileDialog(): File? {
  val dialog = FileDialog(null as Frame?, "Open Log File", FileDialog.LOAD)
  dialog.isVisible = true
  return dialog.file?.let { File(dialog.directory, it) }
}
