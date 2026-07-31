package eivydas.senkus.logcruiser.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eivydas.senkus.logcruiser.index.IndexingProgress
import eivydas.senkus.logcruiser.index.LogFileIndex
import eivydas.senkus.logcruiser.index.OffsetLineReader
import java.awt.FileDialog
import java.io.File
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private sealed class ViewerState {
  data object Idle : ViewerState()

  data class Indexing(val progress: IndexingProgress) : ViewerState()

  data class Ready(val index: LogFileIndex, val reader: OffsetLineReader) : ViewerState()
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
  var state by remember { mutableStateOf<ViewerState>(ViewerState.Idle) }
  val scope = rememberCoroutineScope()
  MaterialTheme(colorScheme = darkColorScheme()) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
      when (val currentState = state) {
        ViewerState.Idle -> {
          IdleScreen(
              onOpenFile = {
                val file = showOpenFileDialog()
                if (file != null) {
                  scope.launch {
                    val index = LogFileIndex(file)
                    val progressJob = scope.launch {
                      index.progress.collect { progress ->
                        state = ViewerState.Indexing(progress)
                      }
                    }
                    index.build()
                    progressJob.cancel()
                    state = ViewerState.Ready(index, OffsetLineReader(file))
                  }
                }
              },
              modifier = modifier,
          )
        }

        is ViewerState.Indexing -> {
          IndexingScreen(
              progress = currentState.progress,
              onCancel = { scope.cancel() },
              modifier = modifier,
          )
        }

        is ViewerState.Ready -> {
          val index = currentState.index
          val reader = currentState.reader

          DisposableEffect(reader) {
            onDispose {
              reader.close()
            }
          }

          LogViewport(
              index = index,
              reader = reader,
              modifier = modifier.fillMaxSize(),
          )
        }
      }
    }
  }
}

@Composable
internal fun IdleScreen(
    onOpenFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Button(onClick = onOpenFile) {
      Text("Open File", style = MaterialTheme.typography.headlineSmall)
    }
  }
}

@Composable
internal fun IndexingScreen(
    progress: IndexingProgress,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier.fillMaxSize().padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    val progressFraction =
        if (progress.totalBytes > 0) {
          progress.bytesRead.toFloat() / progress.totalBytes.toFloat()
        } else {
          0f
        }

    LinearProgressIndicator(
        progress = { progressFraction },
        modifier = Modifier.fillMaxWidth(),
    )

    Text(
        text =
            "Indexing... ${formatBytes(progress.bytesRead)} / ${formatBytes(progress.totalBytes)}",
        modifier = Modifier.padding(top = 8.dp),
    )

    TextButton(onClick = onCancel) {
      Text("Cancel")
    }
  }
}

@Preview
@Composable
internal fun PreviewIdleScreen() {
  MaterialTheme(colorScheme = darkColorScheme()) {
    Surface(color = MaterialTheme.colorScheme.background) {
      IdleScreen(onOpenFile = {})
    }
  }
}

@Preview
@Composable
internal fun PreviewIndexingScreen() {
  MaterialTheme(colorScheme = darkColorScheme()) {
    Surface(color = MaterialTheme.colorScheme.background) {
      IndexingScreen(
          progress = IndexingProgress(50000000, 120000000, false),
          onCancel = {},
      )
    }
  }
}

private fun showOpenFileDialog(): File? {
  val dialog = FileDialog(null as java.awt.Frame?, "Open Log File", FileDialog.LOAD)
  dialog.isVisible = true
  return dialog.file?.let { File(dialog.directory, it) }
}

private fun formatBytes(bytes: Long): String {
  val units = arrayOf("B", "KB", "MB", "GB", "TB")
  var value = bytes.toDouble()
  var unitIndex = 0
  while (value >= 1024 && unitIndex < units.size - 1) {
    value /= 1024
    unitIndex++
  }
  return "%.1f %s".format(value, units[unitIndex])
}
