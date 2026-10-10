package eivydas.senkus.logcruiser.storage

import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

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

  internal fun writeAtomically(directory: Path, fileName: String, content: String) {
    Files.createDirectories(directory)
    val file = directory.resolve(fileName)
    val temporaryFile = directory.resolve("$fileName.tmp")
    try {
      Files.write(temporaryFile, content.toByteArray(Charsets.UTF_8))
      try {
        Files.move(temporaryFile, file, ATOMIC_MOVE, REPLACE_EXISTING)
      } catch (_: AtomicMoveNotSupportedException) {
        Files.move(temporaryFile, file, REPLACE_EXISTING)
      }
    } finally {
      Files.deleteIfExists(temporaryFile)
    }
  }

  internal fun moveCorruptFile(directory: Path, fileName: String) {
    try {
      Files.move(
          directory.resolve(fileName),
          directory.resolve("$fileName.corrupt"),
          REPLACE_EXISTING,
      )
    } catch (_: IOException) {}
  }
}
