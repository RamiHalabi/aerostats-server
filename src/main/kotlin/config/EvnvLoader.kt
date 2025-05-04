package config

import java.io.File
import java.io.FileNotFoundException

object EnvLoader {
    private val envMap: Map<String, String> by lazy {
        loadEnvFile()
    }

    private fun loadEnvFile(): Map<String, String> {
        val envMap = mutableMapOf<String, String>()
        try {
            // Look for .env file in the project root or resources directory
            val envFile = File(".env").takeIf { it.exists() }
                ?: File("src/main/resources/.env").takeIf { it.exists() }
                ?: return emptyMap()

            envFile.readLines().forEach { line ->
                // Skip comments and empty lines
                if (!line.startsWith("#") && line.isNotBlank() && line.contains("=")) {
                    val (key, value) = line.split("=", limit = 2)
                    envMap[key.trim()] = value.trim().removeSurroundingQuotes()
                }
            }
        } catch (e: FileNotFoundException) {
            // File not found, return empty map
        } catch (e: Exception) {
            println("Error loading .env file: ${e.message}")
        }
        return envMap
    }

    // Helper function to get a value from .env file
    fun get(key: String): String? = envMap[key]

    // Helper extension to remove quotes from values if present
    private fun String.removeSurroundingQuotes(): String {
        return if ((startsWith("\"") && endsWith("\"")) || (startsWith("'") && endsWith("'"))) {
            substring(1, length - 1)
        } else {
            this
        }
    }
}