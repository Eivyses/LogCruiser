package eivydas.senkus.logcruiser.storage

import eivydas.senkus.logcruiser.model.ProfileDef
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class ProfileStore(private val directory: Path = AppStorage.configDir()) {
  private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
  }
  private val file = directory.resolve("profiles.json")
  private val _profiles = MutableStateFlow(load())

  val profiles: StateFlow<List<ProfileDef>> = _profiles.asStateFlow()

  fun save(profile: ProfileDef) {
    _profiles.value =
        _profiles.value.let { profiles ->
          val index = profiles.indexOfFirst { it.id == profile.id }
          if (index < 0) profiles + profile
          else profiles.toMutableList().apply { set(index, profile) }
        }
    persist()
  }

  fun delete(id: String) {
    _profiles.value = _profiles.value.filterNot { it.id == id }
    persist()
  }

  private fun load(): List<ProfileDef> =
      try {
        if (Files.notExists(file)) return emptyList()
        json.decodeFromString(String(Files.readAllBytes(file), Charsets.UTF_8))
      } catch (_: SerializationException) {
        AppStorage.moveCorruptFile(directory, "profiles.json")
        emptyList()
      } catch (_: IllegalArgumentException) {
        AppStorage.moveCorruptFile(directory, "profiles.json")
        emptyList()
      } catch (_: IOException) {
        emptyList()
      }

  private fun persist() =
      AppStorage.writeAtomically(directory, "profiles.json", json.encodeToString(_profiles.value))
}
