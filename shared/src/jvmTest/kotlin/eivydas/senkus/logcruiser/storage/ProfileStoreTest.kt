package eivydas.senkus.logcruiser.storage

import eivydas.senkus.logcruiser.model.FilterDef
import eivydas.senkus.logcruiser.model.FilterKind
import eivydas.senkus.logcruiser.model.FilterType
import eivydas.senkus.logcruiser.model.IncludeMatchMode
import eivydas.senkus.logcruiser.model.ProfileDef
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileStoreTest {
  @Test
  fun `saved profile loads from a new store`() = withTempDirectory { directory ->
    val profile = profile()

    ProfileStore(directory).save(profile)

    assertEquals(listOf(profile), ProfileStore(directory).profiles.value)
  }

  @Test
  fun `saving the same id replaces the profile`() = withTempDirectory { directory ->
    val store = ProfileStore(directory)
    store.save(profile(name = "First"))
    store.save(profile(name = "Second"))

    assertEquals(listOf(profile(name = "Second")), store.profiles.value)
  }

  @Test
  fun `delete persists removal`() = withTempDirectory { directory ->
    val profile = profile()
    ProfileStore(directory).save(profile)

    ProfileStore(directory).apply { delete(profile.id) }

    assertTrue(ProfileStore(directory).profiles.value.isEmpty())
  }

  @Test
  fun `invalid json is moved aside`() = withTempDirectory { directory ->
    Files.createDirectories(directory)
    Files.write(directory.resolve("profiles.json"), "not json".toByteArray(Charsets.UTF_8))

    assertEquals(emptyList(), ProfileStore(directory).profiles.value)
    assertTrue(Files.exists(directory.resolve("profiles.json.corrupt")))
  }

  @Test
  fun `persist leaves no temporary file`() = withTempDirectory { directory ->
    ProfileStore(directory).save(profile())

    assertTrue(Files.exists(directory.resolve("profiles.json")))
    assertFalse(Files.exists(directory.resolve("profiles.json.tmp")))
  }

  private fun profile(name: String = "Production") =
      ProfileDef(
          id = "profile-1",
          name = name,
          filters =
              listOf(
                  FilterDef(
                      id = "filter-1",
                      kind = FilterKind.Include,
                      type = FilterType.Substring,
                      value = "error",
                      enabled = false,
                  )
              ),
          includeMatchMode = IncludeMatchMode.All,
          createdAt = 1234L,
      )

  private fun withTempDirectory(block: (Path) -> Unit) {
    val directory = Files.createTempDirectory("logcruiser-profile-store")
    try {
      block(directory)
    } finally {
      Files.walk(directory).use { paths ->
        paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
      }
    }
  }
}
