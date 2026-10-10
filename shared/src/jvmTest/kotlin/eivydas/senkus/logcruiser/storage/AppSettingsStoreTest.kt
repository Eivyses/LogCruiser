package eivydas.senkus.logcruiser.storage

import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSettingsStoreTest {
  @Test
  fun `theme preference defaults to dark`() = withTempDirectory { directory ->
    assertTrue(AppSettingsStore(directory).isDarkTheme.value)
  }

  @Test
  fun `theme preference persists across store instances`() = withTempDirectory { directory ->
    AppSettingsStore(directory).setDarkTheme(false)

    assertFalse(AppSettingsStore(directory).isDarkTheme.value)
  }

  private fun withTempDirectory(block: (Path) -> Unit) {
    val directory = Files.createTempDirectory("logcruiser-settings-store")
    try {
      block(directory)
    } finally {
      Files.walk(directory).use { paths ->
        paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
      }
    }
  }
}
