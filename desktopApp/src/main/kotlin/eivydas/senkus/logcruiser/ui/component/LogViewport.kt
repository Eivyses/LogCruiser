package eivydas.senkus.logcruiser.ui.component

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import eivydas.senkus.logcruiser.index.FilteredLogFileIndex
import eivydas.senkus.logcruiser.index.OffsetLineReader
import eivydas.senkus.logcruiser.ui.LogCruiserPreview

private val LINE_NUMBER_WIDTH = 90.dp

@Composable
fun LogViewport(
    filteredIndex: FilteredLogFileIndex,
    reader: OffsetLineReader,
    modifier: Modifier = Modifier,
) {
  val horizontalScrollState = rememberScrollState()
  val listState = rememberLazyListState()

  Row(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
      Box(
          modifier = Modifier.weight(1f).fillMaxWidth().horizontalScroll(horizontalScrollState),
      ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxHeight(),
        ) {
          items(count = filteredIndex.lineCount, key = { filteredIndex.sourceLineIndexAt(it) }) {
              filteredLineIndex ->
            // The list position is filtered; reading still needs the corresponding source line.
            val sourceLineIndex = filteredIndex.sourceLineIndexAt(filteredLineIndex)
            LogLine(
                lineNumber = sourceLineIndex + 1,
                lineText = reader.readLine(filteredIndex.sourceIndex, sourceLineIndex),
                horizontalScrollState = horizontalScrollState,
            )
          }
        }
      }

      HorizontalScrollbar(
          adapter = rememberScrollbarAdapter(horizontalScrollState),
          style = scrollbarStyle(),
          modifier = Modifier.fillMaxWidth().height(16.dp).padding(horizontal = 5.dp),
      )
    }

    Box(modifier = Modifier.padding(horizontal = 5.dp).width(16.dp).fillMaxHeight()) {
      VerticalScrollbar(
          adapter = rememberScrollbarAdapter(listState),
          style = scrollbarStyle(),
          modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
      )
    }
  }
}

@Composable
private fun LogLine(
    lineNumber: Int,
    lineText: String,
    horizontalScrollState: ScrollState,
) {
  Box {
    Row {
      Spacer(modifier = Modifier.requiredWidth(LINE_NUMBER_WIDTH))
      Text(
          text = lineText,
          modifier = Modifier.padding(start = 4.dp),
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          softWrap = false,
      )
    }
    Row(
        modifier =
            Modifier.offset { IntOffset(x = horizontalScrollState.value, y = 0) }
                .background(MaterialTheme.colorScheme.background),
    ) {
      Box(
          modifier = Modifier.requiredWidth(LINE_NUMBER_WIDTH).padding(start = 4.dp, end = 12.dp),
          contentAlignment = Alignment.CenterEnd,
      ) {
        Text(
            text = "%8d".format(lineNumber),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            softWrap = false,
        )
      }
      VerticalDivider(
          color = MaterialTheme.colorScheme.outlineVariant,
          thickness = 1.dp,
          modifier = Modifier.fillMaxHeight(),
      )
    }
  }
}

@Composable
private fun scrollbarStyle(): ScrollbarStyle {
  val background = MaterialTheme.colorScheme.background
  val isDark = (background.red + background.green + background.blue) / 3f < 0.5f
  val color = if (isDark) Color.White else Color.Black
  return ScrollbarStyle(
      minimalHeight = 16.dp,
      thickness = 12.dp,
      shape = RoundedCornerShape(4.dp),
      hoverDurationMillis = 300,
      unhoverColor = color.copy(alpha = 0.12f),
      hoverColor = color.copy(alpha = 0.50f),
  )
}

@Preview(name = "Dark Mode")
@Preview(name = "Light Mode")
@Composable
private fun PreviewLogLinesLight() {
  LogCruiserPreview(isDark = false) {
    PreviewLogLines()
  }
}

@Preview
@Composable
private fun PreviewLogLinesDark() {
  LogCruiserPreview(isDark = true) {
    PreviewLogLines()
  }
}

@Composable
private fun PreviewLogLines() {
  val verticalScrollState = rememberScrollState()
  val horizontalScrollState = rememberScrollState()
  Column(Modifier.verticalScroll(state = verticalScrollState)) {
    LogLine(
        lineNumber = 1,
        lineText = "[2024-01-15 10:30:45] [INFO] Application started successfully",
        horizontalScrollState = horizontalScrollState,
    )
    LogLine(
        lineNumber = 2,
        lineText = "[2024-01-15 10:30:46] [DEBUG] Loading configuration from /etc/app/config.yaml",
        horizontalScrollState = horizontalScrollState,
    )
    LogLine(
        lineNumber = 3,
        lineText = "[2024-01-15 10:30:47] [WARN] Connection pool usage is at 75%",
        horizontalScrollState = horizontalScrollState,
    )
    LogLine(
        lineNumber = 1234567,
        lineText = "[2024-01-15 10:30:48] [ERROR] Failed to process request: timeout after 30s",
        horizontalScrollState = horizontalScrollState,
    )
  }
}
