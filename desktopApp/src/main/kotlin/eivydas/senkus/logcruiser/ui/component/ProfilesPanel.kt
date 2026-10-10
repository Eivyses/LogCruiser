package eivydas.senkus.logcruiser.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eivydas.senkus.logcruiser.model.FilterDef
import eivydas.senkus.logcruiser.model.FilterKind
import eivydas.senkus.logcruiser.model.FilterType
import eivydas.senkus.logcruiser.model.IncludeMatchMode
import eivydas.senkus.logcruiser.model.ProfileDef
import eivydas.senkus.logcruiser.ui.LogCruiserPreview

@Composable
fun ProfilesPanel(
    profiles: List<ProfileDef>,
    currentFilters: List<FilterDef>,
    currentIncludeMatchMode: IncludeMatchMode,
    onSave: (String) -> Unit,
    onApply: (ProfileDef) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
  var name by rememberSaveable { mutableStateOf("") }

  Column(
      modifier = modifier.fillMaxSize().padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          modifier = Modifier.weight(1f),
          singleLine = true,
          label = { Text("Profile name") },
      )
      Button(
          onClick = {
            onSave(name.trim())
            name = ""
          },
          enabled = name.isNotBlank() && currentFilters.isNotEmpty(),
      ) {
        Text("Save")
      }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      items(items = profiles, key = { it.id }) { profile ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(profile.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${profile.filters.size} filters",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          TextButton(onClick = { onApply(profile) }) { Text("Apply") }
          IconButton(onClick = { onDelete(profile.id) }) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete profile")
          }
        }
      }
    }
  }
}

@Preview(name = "Light")
@Composable
private fun PreviewProfilesPanelLight() {
  PreviewProfilesPanel(isDark = false)
}

@Preview(name = "Dark")
@Composable
private fun PreviewProfilesPanelDark() {
  PreviewProfilesPanel(isDark = true)
}

@Composable
private fun PreviewProfilesPanel(isDark: Boolean) {
  val filter =
      FilterDef(
          id = "error-filter",
          kind = FilterKind.Include,
          type = FilterType.Substring,
          value = "error",
      )
  LogCruiserPreview(isDark = isDark) {
    ProfilesPanel(
        profiles =
            listOf(
                ProfileDef(
                    id = "production",
                    name = "Production logs",
                    filters = listOf(filter),
                )
            ),
        currentFilters = listOf(filter),
        currentIncludeMatchMode = IncludeMatchMode.Any,
        onSave = {},
        onApply = {},
        onDelete = {},
        modifier = Modifier.width(360.dp).height(320.dp),
    )
  }
}
