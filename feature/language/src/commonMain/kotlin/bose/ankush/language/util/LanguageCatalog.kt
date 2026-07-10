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
    suspend fun getAvailableLanguages(fileName: String = "countryConfig.json"): List<String> =
        Json
            .decodeFromString<CountryConfig>(
                Res.readBytes("files/$fileName").decodeToString(),
            ).languages
}
