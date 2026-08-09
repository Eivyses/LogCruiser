package eivydas.senkus.logcruiser.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.rememberComponentRectPositionProvider

@Composable
fun MenuBarDropdown(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    menuItems: List<MenuItem>,
) {
  if (!expanded) return

  Popup(
      onDismissRequest = onDismissRequest,
      popupPositionProvider =
          rememberComponentRectPositionProvider(
              anchor = Alignment.BottomStart,
              alignment = Alignment.BottomEnd,
          ),
      properties = PopupProperties(focusable = false),
  ) {
    Surface(
        modifier = Modifier.width(IntrinsicSize.Max),
        shape = MenuDefaults.shape,
        color = MenuDefaults.containerColor,
        tonalElevation = MenuDefaults.TonalElevation,
        shadowElevation = MenuDefaults.ShadowElevation,
    ) {
      Column(
          modifier = Modifier.padding(vertical = 8.dp).verticalScroll(rememberScrollState()),
      ) {
        menuItems.forEach { menuItem ->
          DropdownMenuItem(
              text = { Text(menuItem.text) },
              onClick = {
                onDismissRequest()
                menuItem.onClick()
              },
          )
        }
      }
    }
  }
}
