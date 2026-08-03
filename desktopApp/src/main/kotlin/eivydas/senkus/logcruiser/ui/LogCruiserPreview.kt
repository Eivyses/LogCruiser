package eivydas.senkus.logcruiser.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun LogCruiserPreview(
    isDark: Boolean = false,
    content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = if (isDark) darkColorScheme() else lightColorScheme()) {
    Surface(color = MaterialTheme.colorScheme.background) {
      content()
    }
  }
}
