package eivydas.senkus.logcruiser.model

import kotlinx.serialization.Serializable

@Serializable
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

@Serializable
enum class FilterKind {
  Include,
  Exclude,
}

@Serializable
enum class FilterType {
  Substring,
}
