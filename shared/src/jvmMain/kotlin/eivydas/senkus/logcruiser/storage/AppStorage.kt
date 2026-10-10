package eivydas.senkus.logcruiser.storage

import java.nio.file.Path
import java.nio.file.Paths

object AppStorage {
  fun configDir(): Path {
    val home = Paths.get(System.getProperty("user.home"))
    val osName = System.getProperty("os.name").lowercase()

    return when {
      "win" in osName ->
          System.getenv("LOCALAPPDATA")
              ?.takeIf(String::isNotBlank)
              ?.let { Paths.get(it) }
              ?.resolve("LogCruiser")
              ?: home.resolve("AppData").resolve("Local").resolve("LogCruiser")
      "mac" in osName ->
          home.resolve("Library").resolve("Application Support").resolve("LogCruiser")
      else ->
          System.getenv("XDG_CONFIG_HOME")
              ?.takeIf(String::isNotBlank)
              ?.let { Paths.get(it) }
              ?.resolve("logcruiser") ?: home.resolve(".config").resolve("logcruiser")
    }
  }
}
