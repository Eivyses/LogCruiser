package eivydas.senkus.logcruiser.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfileDef(
    val id: String,
    val name: String,
    val filters: List<FilterDef>,
    val includeMatchMode: IncludeMatchMode = IncludeMatchMode.Any,
    val createdAt: Long = 0L,
) {
  init {
    require(name.isNotBlank()) { "Profile name must not be blank" }
  }
}
