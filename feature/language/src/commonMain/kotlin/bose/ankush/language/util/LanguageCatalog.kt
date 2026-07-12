package bose.ankush.language.util

import bose.ankush.language.generated.resources.Res
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class CountryConfig(
    val languages: List<String> = emptyList(),
)

/** Reads the bundled country/language config, identically on Android and iOS via Compose Resources. */
object LanguageCatalog {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getAvailableLanguages(fileName: String = "countryConfig.json"): List<String> =
        json
            .decodeFromString<CountryConfig>(
                Res.readBytes("files/$fileName").decodeToString(),
            ).languages
}
