package eivydas.senkus.logcruiser.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ProfileDefTest {
  @Test
  fun `filter and profile round trip through json`() {
    val filter =
        FilterDef(
            id = "filter-1",
            kind = FilterKind.Exclude,
            type = FilterType.Substring,
            value = "debug",
            enabled = false,
            caseSensitive = true,
            targetColumn = "level",
        )
    val profile =
        ProfileDef(
            id = "profile-1",
            name = "Production",
            filters = listOf(filter),
            includeMatchMode = IncludeMatchMode.All,
            createdAt = 1234L,
        )

    assertEquals(filter, Json.decodeFromString<FilterDef>(Json.encodeToString(filter)))
    assertEquals(profile, Json.decodeFromString<ProfileDef>(Json.encodeToString(profile)))
  }

  @Test
  fun `omitted optional fields use defaults`() {
    val filter =
        Json.decodeFromString<FilterDef>(
            """{"id":"filter-1","kind":"Include","type":"Substring","value":"error"}"""
        )
    val profile =
        Json.decodeFromString<ProfileDef>("""{"id":"profile-1","name":"Production","filters":[]}""")

    assertEquals(true, filter.enabled)
    assertEquals(false, filter.caseSensitive)
    assertEquals(null, filter.targetColumn)
    assertEquals(IncludeMatchMode.Any, profile.includeMatchMode)
    assertEquals(0L, profile.createdAt)
  }
}
