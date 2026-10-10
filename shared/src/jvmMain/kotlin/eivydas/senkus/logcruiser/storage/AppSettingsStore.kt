package eivydas.senkus.logcruiser.storage

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private const val SETTINGS_FILE = "settings.json"

@Serializable private data class AppSettings(val isDarkTheme: Boolean = true)

class AppSettingsStore(private val directory: Path = AppStorage.configDir()) {
  private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
  }
  private val file = directory.resolve(SETTINGS_FILE)
  private val _isDarkTheme = MutableStateFlow(loadThemePreference())

  val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

  fun setDarkTheme(isDarkTheme: Boolean) {
    _isDarkTheme.value = isDarkTheme
    AppStorage.writeAtomically(
        directory,
        SETTINGS_FILE,
        json.encodeToString(AppSettings(isDarkTheme)),
    )
  }

  private fun loadThemePreference(): Boolean =
      try {
        if (Files.notExists(file)) return true
        json
            .decodeFromString<AppSettings>(String(Files.readAllBytes(file), Charsets.UTF_8))
            .isDarkTheme
      } catch (_: SerializationException) {
        AppStorage.moveCorruptFile(directory, SETTINGS_FILE)
        true
      } catch (_: IllegalArgumentException) {
        AppStorage.moveCorruptFile(directory, SETTINGS_FILE)
        true
      } catch (_: IOException) {
        true
      }
}
