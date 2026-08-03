package eivydas.senkus.logcruiser.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eivydas.senkus.logcruiser.index.IndexingProgress
import eivydas.senkus.logcruiser.ui.LogCruiserPreview

@Composable
internal fun IndexingScreen(
    progress: IndexingProgress,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier.padding(24.dp),
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

internal fun formatBytes(bytes: Long): String {
  val units = arrayOf("B", "KB", "MB", "GB", "TB")
  var value = bytes.toDouble()
  var unitIndex = 0
  while (value >= 1024 && unitIndex < units.size - 1) {
    value /= 1024
    unitIndex++
  }
  return "%.1f %s".format(value, units[unitIndex])
}

@Preview
@Composable
private fun PreviewIndexingScreen() {
  LogCruiserPreview {
    IndexingScreen(
        progress = IndexingProgress(50000000, 120000000, false),
        onCancel = {},
    )
  }
}
