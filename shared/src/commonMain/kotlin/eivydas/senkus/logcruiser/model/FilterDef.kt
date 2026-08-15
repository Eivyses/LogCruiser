package eivydas.senkus.logcruiser.model

data class FilterDef(
    val id: String,
    val kind: FilterKind,
    val type: FilterType,
    val value: String,
    val enabled: Boolean = true,
    val caseSensitive: Boolean = false,
    val targetColumn: String? = null,
) {
  init {
    require(value.isNotBlank()) { "Filter value must not be blank" }
  }
}

enum class FilterKind {
  Include,
  Exclude,
}

enum class FilterType {
  Substring,
}
