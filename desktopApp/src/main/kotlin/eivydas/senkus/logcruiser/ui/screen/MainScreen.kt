package eivydas.senkus.logcruiser.ui.screen

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import eivydas.senkus.logcruiser.model.ProfileDef
import eivydas.senkus.logcruiser.session.LogSessionHolder
import eivydas.senkus.logcruiser.session.ViewerState
import eivydas.senkus.logcruiser.storage.ProfileStore
import eivydas.senkus.logcruiser.ui.component.*
import eivydas.senkus.logcruiser.ui.find.FindBarController
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.util.UUID

@Composable
fun MainScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val session = remember { LogSessionHolder() }
  val profileStore = remember { ProfileStore() }
  val scope = rememberCoroutineScope()
  val findController = remember { FindBarController(scope) }

  DisposableEffect(session) {
    onDispose { session.dispose() }
  }

  val state by session.state.collectAsState()
  val filters by session.filters.collectAsState()
  val includeMatchMode by session.includeMatchMode.collectAsState()
  val quickFilter by session.quickFilter.collectAsState()
  val profiles by profileStore.profiles.collectAsState()
  val darkLightString = if (isDarkTheme) "light" else "dark"

  LaunchedEffect(state) { findController.onViewportChanged(state as? ViewerState.Ready) }

  val openFile: () -> Unit = {
    showOpenFileDialog()?.let(session::openFile)
  }
  val isFiltering = (state as? ViewerState.Ready)?.isFiltering == true
  val readyState = state as? ViewerState.Ready

  val menuItems =
      listOf(
          MenuItem(group = "Files", text = "Open file", onClick = openFile),
          MenuItem(
              group = "Options",
              text = "Change theme to $darkLightString",
              onClick = onToggleTheme,
          ),
      )
  val profilesSidebarItems =
      listOf(
          SideBarItem(
              id = "profiles",
              title = "Profiles",
              icon = { Text("P") },
              content = {
                ProfilesPanel(
                    profiles = profiles,
                    currentFilters = filters,
                    currentIncludeMatchMode = includeMatchMode,
                    onSave = { name ->
                      profileStore.save(
                          ProfileDef(
                              id = UUID.randomUUID().toString(),
                              name = name,
                              filters = filters,
                              includeMatchMode = includeMatchMode,
                              createdAt = System.currentTimeMillis(),
                          )
                      )
                    },
                    onApply = { profile ->
                      session.setFilters(profile.filters, profile.includeMatchMode)
                    },
                    onDelete = profileStore::delete,
                )
              },
          )
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
      FindShortcutContainer(state = state, findController = findController) {
        Column(modifier = Modifier.fillMaxSize()) {
          MenuBarRow(menuItems = menuItems)
          Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxSize()) {
              SideBar(
                  side = SideBarSide.Left,
                  items = profilesSidebarItems,
              )
              Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (state is ViewerState.Ready) {
                  QuickFilterBar(
                      appliedValue = quickFilter?.value,
                      isFiltering = isFiltering,
                      onApply = session::setQuickFilterText,
                  )
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                  MainWorkArea(
                      state = state,
                      onOpenFile = openFile,
                      onCancelIndexing = session::cancelIndexing,
                      findController = findController,
                  )
                  if (findController.isOpen && state is ViewerState.Ready) {
                    FindBar(
                        controller = findController,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    )
                  }
                }
              }
              SideBar(
                  side = SideBarSide.Right,
                  items = filterSidebarItems,
              )
            }
          }
          StatusBar(
              totalLines = readyState?.filteredIndex?.sourceIndex?.lineCount,
              visibleLines = readyState?.filteredIndex?.lineCount,
          )
        }
      }
    }
  }
}

@Composable
private fun FindShortcutContainer(
    state: ViewerState,
    findController: FindBarController,
    content: @Composable () -> Unit,
) {
  val rootFocusRequester = remember { FocusRequester() }
  LaunchedEffect(Unit) { rootFocusRequester.requestFocus() }
  LaunchedEffect(findController.isOpen) {
    if (!findController.isOpen) rootFocusRequester.requestFocus()
  }

  Box(
      modifier =
          Modifier.fillMaxSize().focusRequester(rootFocusRequester).focusable().onPreviewKeyEvent {
              event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when {
              event.isCtrlPressed && event.key == Key.F && state is ViewerState.Ready -> {
                findController.open()
                true
              }
              event.key == Key.Escape && findController.isOpen -> {
                findController.close()
                true
              }
              else -> false
            }
          }
  ) {
    content()
  }
}

@Composable
private fun MainWorkArea(
    state: ViewerState,
    onOpenFile: () -> Unit,
    onCancelIndexing: () -> Unit,
    findController: FindBarController,
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
          findController = findController,
      )
    }
  }
}

private fun showOpenFileDialog(): File? {
  val dialog = FileDialog(null as Frame?, "Open Log File", FileDialog.LOAD)
  dialog.isVisible = true
  return dialog.file?.let { File(dialog.directory, it) }
}
