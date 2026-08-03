package eivydas.senkus.logcruiser.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import eivydas.senkus.logcruiser.ui.LogCruiserPreview

@Composable
internal fun IdleScreen(
    onOpenFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier,
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Button(onClick = onOpenFile) {
      Text("Open File", style = MaterialTheme.typography.headlineSmall)
    }
  }
}

@Preview
@Composable
private fun PreviewIdleScreen() {
  LogCruiserPreview {
    IdleScreen(onOpenFile = {})
  }
}
