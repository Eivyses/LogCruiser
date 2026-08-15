package eivydas.senkus.logcruiser.index

internal class IntArrayBuilder(initialCapacity: Int = 1024) {
  private var data = IntArray(initialCapacity.coerceAtLeast(1))
  private var size = 0

  fun add(value: Int) {
    if (size == data.size) {
      data = data.copyOf(data.size * 2)
    }
    data[size++] = value
  }

  fun toArray(): IntArray = data.copyOf(size)
}
