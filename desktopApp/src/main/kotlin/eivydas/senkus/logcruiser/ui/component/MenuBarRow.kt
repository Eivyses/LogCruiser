package eivydas.senkus.logcruiser.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eivydas.senkus.logcruiser.ui.LogCruiserPreview

private val MENU_BAR_HEIGHT = 32.dp
private val MENU_ITEM_HORIZONTAL_PADDING = 8.dp

data class MenuItem(val group: String, val text: String, val onClick: () -> Unit)

@Composable
fun MenuBarRow(
    menuItems: List<MenuItem>,
    modifier: Modifier = Modifier,
) {
  var expandedGroup by remember { mutableStateOf<String?>(null) }

  Row(
      modifier =
          modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .height(MENU_BAR_HEIGHT),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    menuItems
        .groupBy { it.group }
        .forEach { (group, groupItems) ->
          val interactionSource = remember { MutableInteractionSource() }
          val hovered by interactionSource.collectIsHoveredAsState()

          LaunchedEffect(hovered, expandedGroup, group) {
            if (hovered && expandedGroup != null && expandedGroup != group) {
              expandedGroup = group
            }
          }

          Box(modifier = Modifier.height(MENU_BAR_HEIGHT)) {
            TextButton(
                onClick = { expandedGroup = group },
                modifier = Modifier.height(MENU_BAR_HEIGHT),
                contentPadding =
                    PaddingValues(horizontal = MENU_ITEM_HORIZONTAL_PADDING, vertical = 0.dp),
                interactionSource = interactionSource,
            ) {
              Text(group)
            }
            MenuBarDropdown(
                expanded = expandedGroup == group,
                onDismissRequest = { expandedGroup = null },
                menuItems = groupItems,
            )
          }
        }
  }
}

@Preview
@Composable
private fun PreviewMenuBarRowDark() {
  LogCruiserPreview(isDark = true) {
    PreviewMenuItems()
  }
}

@Preview
@Composable
private fun PreviewMenuBarRowLight() {
  LogCruiserPreview(isDark = false) {
    PreviewMenuItems()
  }
}

@Composable
private fun PreviewMenuItems() {
  val menuItems =
      listOf(
          MenuItem(group = "Files", text = "Open", onClick = {}),
          MenuItem(group = "Tools", text = "Test", onClick = {}),
          MenuItem(group = "Help", text = "About", onClick = {}),
      )
  MenuBarRow(
      menuItems = menuItems,
      modifier = Modifier.fillMaxWidth(),
  )
}
