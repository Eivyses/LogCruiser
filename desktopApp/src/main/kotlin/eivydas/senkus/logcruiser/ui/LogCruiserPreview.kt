package eivydas.senkus.logcruiser.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun LogCruiserPreview(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = darkColorScheme()) {
    Surface(color = MaterialTheme.colorScheme.background) {
      content()
    }
  }
}
