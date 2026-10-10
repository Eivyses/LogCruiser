package eivydas.senkus.logcruiser.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eivydas.senkus.logcruiser.ui.LogCruiserPreview
import eivydas.senkus.logcruiser.ui.find.FindBarController

@Composable
fun FindBar(controller: FindBarController, modifier: Modifier = Modifier) {
  val focusRequester = remember { FocusRequester() }
  val fieldShape = RoundedCornerShape(6.dp)
  var fieldValue by remember(controller) { mutableStateOf(TextFieldValue(controller.query)) }
  val counterText =
      when {
        controller.matches == null -> ""
        controller.matches!!.isEmpty() && controller.isScanning -> "…"
        controller.matches!!.isEmpty() -> "No results"
        else -> {
          val current = (controller.currentIndex + 1).coerceAtLeast(1)
          if (controller.isScanning) "$current/${controller.matches!!.size}…"
          else "$current/${controller.matches!!.size}"
        }
      }

  LaunchedEffect(controller.focusRequestId) {
    focusRequester.requestFocus()
    fieldValue = fieldValue.copy(selection = TextRange(0, fieldValue.text.length))
  }

  Surface(
      modifier = modifier.width(480.dp).height(40.dp),
      shape = RoundedCornerShape(8.dp),
      tonalElevation = 4.dp,
      shadowElevation = 8.dp,
  ) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      BasicTextField(
          value = fieldValue,
          onValueChange = { value ->
            fieldValue = value
            controller.onQueryChange(value.text)
          },
          modifier =
              Modifier.weight(1f)
                  .height(32.dp)
                  .focusRequester(focusRequester)
                  .onFocusChanged { state ->
                    if (state.isFocused) {
                      fieldValue = fieldValue.copy(selection = TextRange(0, fieldValue.text.length))
                    }
                  }
                  .onKeyEvent { event ->
                    when (event.type) {
                      KeyEventType.KeyDown ->
                          when (event.key) {
                            Key.Enter -> {
                              if (event.isShiftPressed) controller.previous() else controller.next()
                              true
                            }
                            Key.Escape -> {
                              controller.close()
                              true
                            }
                            else -> false
                          }
                      else -> false
                    }
                  }
                  .semantics { contentDescription = "Find" },
          textStyle =
              MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          singleLine = true,
          decorationBox = { innerTextField ->
            Row(
                modifier =
                    Modifier.fillMaxSize()
                        .clip(fieldShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, fieldShape)
                        .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                  Icons.Filled.Search,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Box(modifier = Modifier.weight(1f)) {
                if (fieldValue.text.isEmpty()) {
                  Text(
                      text = "Find",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                innerTextField()
              }
            }
          },
      )
      Text(
          text = counterText,
          modifier = Modifier.width(88.dp),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          softWrap = false,
      )
      IconActionButton(description = "Previous match", onClick = controller::previous) {
        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null)
      }
      IconActionButton(description = "Next match", onClick = controller::next) {
        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
      }
      IconActionButton(description = "Close find", onClick = controller::close) {
        Icon(Icons.Filled.Close, contentDescription = null)
      }
    }
  }
}

@Composable
private fun IconActionButton(
    description: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
  Box(
      modifier =
          Modifier.size(40.dp)
              .clip(CircleShape)
              .clickable(role = Role.Button, onClick = onClick)
              .semantics { contentDescription = description },
      contentAlignment = Alignment.Center,
  ) {
    content()
  }
}

@Preview(name = "Light")
@Composable
private fun PreviewFindBarLight() {
  PreviewFindBar(isDark = false)
}

@Preview(name = "Dark")
@Composable
private fun PreviewFindBarDark() {
  PreviewFindBar(isDark = true)
}

@Composable
private fun PreviewFindBar(isDark: Boolean) {
  val scope = rememberCoroutineScope()
  val controller = remember { FindBarController(scope) }

  LogCruiserPreview(isDark = isDark) {
    FindBar(
        controller = controller,
        modifier = Modifier.padding(16.dp),
    )
  }
}
