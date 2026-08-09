package eivydas.senkus.logcruiser.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.awt.Cursor

private val SIDEBAR_STRIPE_WIDTH = 48.dp
private val SIDEBAR_DEFAULT_WIDTH = 280.dp
private val SIDEBAR_MIN_WIDTH = 200.dp
private val SIDEBAR_MAX_WIDTH = 600.dp
private val SIDEBAR_ITEM_HEIGHT = 40.dp
private val SIDEBAR_RESIZE_HIT_WIDTH = 16.dp
private val SIDEBAR_RESIZE_LINE_WIDTH = 1.dp

enum class SideBarSide {
  Left,
  Right,
}

data class SideBarItem(
    val id: String,
    val icon: @Composable () -> Unit,
    val title: String,
    val content: @Composable () -> Unit,
)

@Composable
fun SideBar(
    side: SideBarSide,
    items: List<SideBarItem>,
    modifier: Modifier = Modifier,
    initialWidth: Dp = SIDEBAR_DEFAULT_WIDTH,
    minWidth: Dp = SIDEBAR_MIN_WIDTH,
    maxWidth: Dp = SIDEBAR_MAX_WIDTH,
) {
  var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
  var panelWidth by rememberSaveable {
    mutableStateOf(initialWidth.coerceIn(minWidth, maxWidth))
  }
  val selectedItem = items.firstOrNull { it.id == selectedId }

  Row(modifier = modifier.fillMaxHeight()) {
    if (side == SideBarSide.Right && selectedItem != null) {
      SideBarResizeHandle(
          side = side,
          onResize = { delta ->
            panelWidth = (panelWidth + delta).coerceIn(minWidth, maxWidth)
          },
      )
    }

    if (side == SideBarSide.Right && selectedItem != null) {
      SideBarContent(item = selectedItem, width = panelWidth) { selectedId = null }
    }

    SideBarStripe(
        side = side,
        items = items,
        selectedId = selectedId,
        onItemClick = { item ->
          selectedId = if (selectedId == item.id) null else item.id
        },
    )

    if (side == SideBarSide.Left && selectedItem != null) {
      SideBarContent(item = selectedItem, width = panelWidth) { selectedId = null }
      SideBarResizeHandle(
          side = side,
          onResize = { delta ->
            panelWidth = (panelWidth + delta).coerceIn(minWidth, maxWidth)
          },
      )
    }
  }
}

@Composable
private fun SideBarStripe(
    side: SideBarSide,
    items: List<SideBarItem>,
    selectedId: String?,
    onItemClick: (SideBarItem) -> Unit,
) {
  Surface(
      modifier = Modifier.width(SIDEBAR_STRIPE_WIDTH).fillMaxHeight(),
      color = MaterialTheme.colorScheme.surfaceVariant,
  ) {
    Column(
        modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      items.forEach { item ->
        val selected = item.id == selectedId
        Surface(
            onClick = { onItemClick(item) },
            modifier =
                Modifier.padding(horizontal = 4.dp).fillMaxWidth().height(SIDEBAR_ITEM_HEIGHT),
            shape = RoundedCornerShape(4.dp),
            color =
                if (selected) {
                  MaterialTheme.colorScheme.primaryContainer
                } else {
                  MaterialTheme.colorScheme.surfaceVariant
                },
            contentColor =
                if (selected) {
                  MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                },
        ) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            item.icon()
            if (selected) {
              Box(
                  modifier =
                      Modifier.align(
                              if (side == SideBarSide.Left) {
                                Alignment.CenterEnd
                              } else {
                                Alignment.CenterStart
                              }
                          )
                          .width(3.dp)
                          .fillMaxHeight()
                          .background(MaterialTheme.colorScheme.primary),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SideBarContent(item: SideBarItem, width: Dp, onClose: () -> Unit) {
  Surface(
      modifier = Modifier.width(width).fillMaxHeight(),
      color = MaterialTheme.colorScheme.surface,
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      Row(
          modifier = Modifier.fillMaxWidth().height(48.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
            text = item.title,
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.titleSmall,
        )
        TextButton(onClick = onClose, contentPadding = PaddingValues(horizontal = 12.dp)) {
          Text("X")
        }
      }
      HorizontalDivider()
      Box(modifier = Modifier.weight(1f).fillMaxWidth()) { item.content() }
    }
  }
}

@Composable
private fun SideBarResizeHandle(
    side: SideBarSide,
    onResize: (Dp) -> Unit,
) {
  val density = LocalDensity.current
  val currentOnResize by rememberUpdatedState(onResize)
  val interactionSource = remember { MutableInteractionSource() }
  val hovered by interactionSource.collectIsHoveredAsState()

  Box(
      modifier =
          Modifier.width(SIDEBAR_RESIZE_HIT_WIDTH)
              .fillMaxHeight()
              .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
              .hoverable(interactionSource)
              .pointerInput(side) {
                detectDragGestures { _, dragAmount ->
                  val horizontalDelta = with(density) { dragAmount.x.toDp() }
                  val delta =
                      if (side == SideBarSide.Left) {
                        horizontalDelta
                      } else {
                        -horizontalDelta
                      }
                  currentOnResize(delta)
                }
              },
      contentAlignment = Alignment.Center,
  ) {
    VerticalDivider(
        modifier = Modifier.fillMaxHeight(),
        thickness = SIDEBAR_RESIZE_LINE_WIDTH,
        color =
            if (hovered) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.outlineVariant
            },
    )
  }
}
