package eivydas.senkus.logcruiser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import eivydas.senkus.logcruiser.ui.screen.MainScreen

fun main() = application {
  var isDarkTheme by remember { mutableStateOf(true) }

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
        onToggleTheme = { isDarkTheme = !isDarkTheme },
    )
  }
}
