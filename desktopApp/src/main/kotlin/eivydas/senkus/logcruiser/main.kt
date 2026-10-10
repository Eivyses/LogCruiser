package eivydas.senkus.logcruiser

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import eivydas.senkus.logcruiser.storage.AppSettingsStore
import eivydas.senkus.logcruiser.ui.screen.MainScreen

fun main() = application {
  val settingsStore = remember { AppSettingsStore() }
  val isDarkTheme by settingsStore.isDarkTheme.collectAsState()

  Window(
      onCloseRequest = ::exitApplication,
      title = "LogCruiser",
      state =
          WindowState(
              width = 1700.dp,
              height = 1000.dp,
          ),
  ) {
    MainScreen(
        isDarkTheme = isDarkTheme,
        onToggleTheme = { settingsStore.setDarkTheme(!isDarkTheme) },
    )
  }
}
