package eivydas.senkus.logcruiser.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import eivydas.senkus.logcruiser.session.LogSessionHolder
import eivydas.senkus.logcruiser.session.ViewerState
import eivydas.senkus.logcruiser.ui.component.*
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun MainScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val session = remember { LogSessionHolder() }
  val state by session.state.collectAsState()
  val filters by session.filters.collectAsState()
  val includeMatchMode by session.includeMatchMode.collectAsState()
  val darkLightString = if (isDarkTheme) "light" else "dark"

  DisposableEffect(session) {
    onDispose { session.dispose() }
  }

  val openFile: () -> Unit = {
    showOpenFileDialog()?.let(session::openFile)
  }
  val isFiltering = (state as? ViewerState.Ready)?.isFiltering == true

  val menuItems =
      listOf(
          MenuItem(group = "Files", text = "Open file", onClick = openFile),
          MenuItem(
              group = "Options",
              text = "Change theme to $darkLightString",
              onClick = onToggleTheme,
          ),
      )
  val filterSidebarItems =
      listOf(
          SideBarItem(
              id = "filters",
              title = "Filters",
              icon = { Text("F") },
              content = {
                FiltersPanel(
                    filters = filters,
                    isFiltering = isFiltering,
                    includeMatchMode = includeMatchMode,
                    onAddFilter = session::addFilter,
                    onToggleFilter = session::toggleFilter,
                    onDeleteFilter = session::deleteFilter,
                    onIncludeMatchModeChange = session::setIncludeMatchMode,
                )
              },
          )
      )

  val colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()

  MaterialTheme(colorScheme = colorScheme) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize()) {
      Column(modifier = modifier.fillMaxSize()) {
        MenuBarRow(menuItems = menuItems)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
          Row(modifier = Modifier.fillMaxSize()) {
            SideBar(
                side = SideBarSide.Left,
                items = filterSidebarItems,
            )
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
              MainWorkArea(
                  state = state,
                  onOpenFile = openFile,
                  onCancelIndexing = session::cancelIndexing,
              )
            }
            SideBar(
                side = SideBarSide.Right,
                items = filterSidebarItems,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MainWorkArea(
    state: ViewerState,
    onOpenFile: () -> Unit,
    onCancelIndexing: () -> Unit,
) {
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
          onCancel = onCancelIndexing,
          modifier = Modifier.fillMaxSize(),
      )
    }

    is ViewerState.Ready -> {
      LogViewport(
          filteredIndex = state.filteredIndex,
          reader = state.reader,
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
