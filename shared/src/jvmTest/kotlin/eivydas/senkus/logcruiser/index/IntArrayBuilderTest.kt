package eivydas.senkus.logcruiser.index

import kotlin.test.Test
import kotlin.test.assertEquals

class IntArrayBuilderTest {
  @Test
  fun `builder grows and returns exact values`() {
    val builder = IntArrayBuilder(initialCapacity = 1)
    repeat(10) { builder.add(it) }

    val values = builder.toArray()
    assertEquals(10, values.size)
    values.forEachIndexed { index, value -> assertEquals(index, value) }
  }

  @Test
  fun `empty builder returns empty array`() {
    assertEquals(0, IntArrayBuilder().toArray().size)
  }
}
