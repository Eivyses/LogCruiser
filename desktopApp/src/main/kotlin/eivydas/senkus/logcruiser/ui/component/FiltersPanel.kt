package eivydas.senkus.logcruiser.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eivydas.senkus.logcruiser.model.FilterDef
import eivydas.senkus.logcruiser.model.FilterKind
import eivydas.senkus.logcruiser.model.IncludeMatchMode

@Composable
fun FiltersPanel(
    filters: List<FilterDef>,
    isFiltering: Boolean,
    includeMatchMode: IncludeMatchMode,
    onAddFilter: (FilterKind, String) -> Unit,
    onToggleFilter: (String) -> Unit,
    onDeleteFilter: (String) -> Unit,
    onIncludeMatchModeChange: (IncludeMatchMode) -> Unit,
    modifier: Modifier = Modifier,
) {
  var value by rememberSaveable { mutableStateOf("") }
  var kind by rememberSaveable { mutableStateOf(FilterKind.Include) }
  var kindMenuExpanded by remember { mutableStateOf(false) }

  fun addFilter() {
    val trimmedValue = value.trim()
    if (trimmedValue.isNotEmpty()) {
      onAddFilter(kind, trimmedValue)
      value = ""
    }
  }

  Column(
      modifier = modifier.fillMaxSize().padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      OutlinedTextField(
          value = value,
          onValueChange = { value = it },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          label = { Text("Filter text") },
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
          keyboardActions = KeyboardActions(onDone = { addFilter() }),
      )
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Box {
          Button(onClick = { kindMenuExpanded = true }) { Text(kind.displayName) }
          DropdownMenu(
              expanded = kindMenuExpanded,
              onDismissRequest = { kindMenuExpanded = false },
          ) {
            FilterKind.entries.forEach { option ->
              DropdownMenuItem(
                  text = { Text(option.displayName) },
                  onClick = {
                    kind = option
                    kindMenuExpanded = false
                  },
              )
            }
          }
        }
        IconButton(onClick = ::addFilter, enabled = value.isNotBlank()) {
          Icon(Icons.Filled.Add, contentDescription = "Add filter")
        }
      }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
          text =
              if (includeMatchMode == IncludeMatchMode.All) {
                "Match all contains filters"
              } else {
                "Match any contains filters"
              },
          modifier = Modifier.weight(1f),
      )
      Switch(
          checked = includeMatchMode == IncludeMatchMode.All,
          onCheckedChange = { enabled ->
            onIncludeMatchModeChange(if (enabled) IncludeMatchMode.All else IncludeMatchMode.Any)
          },
      )
    }

    if (isFiltering) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      items(items = filters, key = { it.id }) { filter ->
        FilterRow(
            filter = filter,
            onToggle = { onToggleFilter(filter.id) },
            onDelete = { onDeleteFilter(filter.id) },
        )
      }
    }
  }
}

@Composable
private fun FilterRow(
    filter: FilterDef,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
        text = "${filter.kind.displayName.lowercase()} \"${filter.value}\"",
        modifier = Modifier.weight(1f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    IconButton(onClick = onToggle) {
      Icon(
          imageVector = if (filter.enabled) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
          contentDescription = if (filter.enabled) "Disable filter" else "Enable filter",
          tint =
              if (filter.enabled) {
                MaterialTheme.colorScheme.onSurface
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
      )
    }
    IconButton(onClick = onDelete) {
      Icon(Icons.Filled.Close, contentDescription = "Delete filter")
    }
  }
}

private val FilterKind.displayName: String
  get() =
      when (this) {
        FilterKind.Include -> "Contains"
        FilterKind.Exclude -> "Not contains"
      }
