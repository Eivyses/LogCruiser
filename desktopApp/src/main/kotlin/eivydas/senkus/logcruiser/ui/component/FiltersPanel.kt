package eivydas.senkus.logcruiser.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FiltersPanel(modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.fillMaxSize().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text("Filters", style = MaterialTheme.typography.titleMedium)
    Text("Filter controls will appear here.")
  }
}
