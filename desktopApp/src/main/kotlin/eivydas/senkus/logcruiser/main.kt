package eivydas.senkus.logcruiser

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import eivydas.senkus.logcruiser.ui.MainScreen

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "LogCruiser",
        state = WindowState(
            width = 1200.dp,
            height = 800.dp,
        ),
    ) {
        MainScreen()
    }
}