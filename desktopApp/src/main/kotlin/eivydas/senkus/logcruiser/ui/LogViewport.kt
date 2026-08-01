package eivydas.senkus.logcruiser.ui

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
import androidx.compose.ui.unit.dp
import eivydas.senkus.logcruiser.index.LogFileIndex
import eivydas.senkus.logcruiser.index.OffsetLineReader

private val LINE_NUMBER_WIDTH = 100.dp

private val darkScrollbarStyle =
    ScrollbarStyle(
        minimalHeight = 16.dp,
        thickness = 12.dp,
        shape = RoundedCornerShape(4.dp),
        hoverDurationMillis = 300,
        unhoverColor = Color.White.copy(alpha = 0.12f),
        hoverColor = Color.White.copy(alpha = 0.50f),
    )

@Composable
fun LogViewport(
    index: LogFileIndex,
    reader: OffsetLineReader,
    modifier: Modifier = Modifier,
) {
  val horizontalScrollState = rememberScrollState()
  val listState = rememberLazyListState()

  Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
    ) {
      items(count = index.lineCount, key = { it }) { lineIdx ->
        LogLine(
            lineNumber = lineIdx + 1,
            lineText = reader.readLine(index.offsetsArray, lineIdx),
            horizontalScrollState = horizontalScrollState,
            modifier = Modifier.fillMaxWidth(),
        )
      }
    }

    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(listState),
        style = darkScrollbarStyle,
        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
    )
  }
}

@Composable
private fun LogLine(
    lineNumber: Int,
    lineText: String,
    horizontalScrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
  Row(modifier = modifier) {
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
    Text(
        text = lineText,
        modifier =
            Modifier.weight(1f).padding(start = 4.dp).horizontalScroll(horizontalScrollState),
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        softWrap = false,
    )
  }
}

@Preview
@Composable
private fun PreviewLogLines() {
  val horizontalScrollState = rememberScrollState()

  LogCruiserPreview {
    Column(Modifier.verticalScroll(state = horizontalScrollState)) {
      LogLine(
          lineNumber = 1,
          lineText = "[2024-01-15 10:30:45] [INFO] Application started successfully",
          horizontalScrollState = horizontalScrollState,
      )
      LogLine(
          lineNumber = 2,
          lineText =
              "[2024-01-15 10:30:46] [DEBUG] Loading configuration from /etc/app/config.yaml",
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
}
