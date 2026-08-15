package eivydas.senkus.logcruiser.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FilterDefTest {
  @Test
  fun `blank value is rejected`() {
    assertFailsWith<IllegalArgumentException> {
      filter(value = "")
    }
    assertFailsWith<IllegalArgumentException> {
      filter(value = "   ")
    }
  }

  @Test
  fun `matching defaults are case insensitive and enabled`() {
    val filter = filter(value = "error")

    assertEquals(false, filter.caseSensitive)
    assertEquals(true, filter.enabled)
  }

  private fun filter(value: String): FilterDef =
      FilterDef(
          id = "test",
          kind = FilterKind.Include,
          type = FilterType.Substring,
          value = value,
      )
}
