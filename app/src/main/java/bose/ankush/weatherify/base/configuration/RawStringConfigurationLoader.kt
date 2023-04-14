package bose.ankush.weatherify.base.configuration

import android.content.res.AssetManager

/**
 * A class that loads raw string configuration from the assets using an [AssetManager] of resources.
 * @property assets The AssetManager instance used for loading the raw string.
 */
class RawStringConfigurationLoader(private val assets: AssetManager) {

    /**
     * Loads a raw string from the assets with the given file name.
     * @param fileName The name of the file to be loaded.
     * @return The loaded string content, or null if loading fails.
     */
    fun loadRawString(fileName: String): String? = try {
        assets.open(fileName).bufferedReader().use { it.readText() }
    } catch (_: Exception) {
        null
    }
}