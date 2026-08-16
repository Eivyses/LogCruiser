package eivydas.senkus.logcruiser.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eivydas.senkus.logcruiser.ui.LogCruiserPreview

@Composable
fun QuickFilterBar(
    appliedValue: String?,
    isFiltering: Boolean,
    onApply: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
  var value by rememberSaveable { mutableStateOf(appliedValue.orEmpty()) }

  LaunchedEffect(appliedValue) { value = appliedValue.orEmpty() }

  fun apply() {
    onApply(value)
  }

  Surface(
      modifier = modifier.fillMaxWidth(),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 1.dp,
  ) {
    Column {
      QuickFilterControls(
          value = value,
          onValueChange = { value = it },
          onApply = ::apply,
          onClear = {
            value = ""
            apply()
          },
      )
      if (isFiltering) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
      }
      HorizontalDivider()
    }
  }
}

@Composable
private fun QuickFilterControls(
    value: String,
    onValueChange: (String) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit,
) {
  Column(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      OutlinedTextField(
          value = value,
          onValueChange = onValueChange,
          modifier = Modifier.weight(1f),
          singleLine = true,
          label = { Text("Quick filter") },
          placeholder = { Text("Substring in visible log lines") },
          trailingIcon =
              if (value.isNotEmpty()) {
                {
                  IconButton(onClick = onClear) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear quick filter")
                  }
                }
              } else {
                null
              },
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(onSearch = { onApply() }),
      )
      Button(onClick = onApply, modifier = Modifier.padding(top = 8.dp)) {
        Icon(Icons.Filled.Search, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Apply")
      }
    }
    Text(
        text = "Press Enter or Apply to filter",
        modifier = Modifier.padding(start = 16.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Preview(name = "Light")
@Composable
private fun PreviewQuickFilterBarLight() {
  PreviewQuickFilterBar(isDark = false)
}

@Preview(name = "Dark filtering")
@Composable
private fun PreviewQuickFilterBarDarkFiltering() {
  PreviewQuickFilterBar(isDark = true, value = "timeout", isFiltering = true)
}

@Composable
private fun PreviewQuickFilterBar(
    isDark: Boolean,
    value: String = "",
    isFiltering: Boolean = false,
) {
  LogCruiserPreview(isDark = isDark) {
    QuickFilterBar(
        appliedValue = value,
        isFiltering = isFiltering,
        onApply = {},
        modifier = Modifier.width(800.dp),
    )
  }
}
